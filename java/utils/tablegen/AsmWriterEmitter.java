package utils.tablegen;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2019, Jianping Zeng
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

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TObjectIntHashMap;
import tools.Error;
import tools.Pair;
import tools.TextUtils;
import tools.Util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import static utils.tablegen.AsmWriterEmitter.AsmWriterOperand.OperandType.*;

/**
 * This class defined for generating partial code of AsmWriter, like Intel or
 * AT&T grammar.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public final class AsmWriterEmitter extends TableGenBackend {
  private PrintStream os;

  private static boolean isIdentPart(char ch) {
    return ch >= 'a' && ch <= 'z'
        || ch >= 'A' && ch <= 'Z'
        || ch >= '0' && ch <= '9'
        || ch == '_';
  }

  static class AsmWriterOperand {
    public String getCode() {
      if (opTy == isLiteralTextOperand) {
        if (str.length() == 1)
          return "os.print('" + str + "');";
        return "os.print(\"" + str + "\");";
      }

      if (opTy == isLiteralStatementOperand)
        return str;

      String result = str + "(mi";
      if (miOpNo != ~0)
        result += ", " + miOpNo;
      if (!miModifier.isEmpty())
        result += ", \"" + miModifier + '"';
      return result + "); ";
    }

    enum OperandType {
      isLiteralTextOperand,
      isMachineInstrOperand,
      isLiteralStatementOperand;
    }

    String str;

    int miOpNo;

    String miModifier;

    OperandType opTy;

    AsmWriterOperand(String litStr) {
      this(litStr, isLiteralTextOperand);
    }

    public AsmWriterOperand(String litStr, OperandType opTy) {
      str = litStr;
      this.opTy = opTy;
    }

    AsmWriterOperand(String printer, int opNo, String modifier, OperandType opTy) {
      this.opTy = opTy;
      str = printer;
      miModifier = modifier;
      miOpNo = opNo;
    }

    AsmWriterOperand(String printer, int opNo, String modifier) {
      this(printer, opNo, modifier, isMachineInstrOperand);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null)
        return false;
      if (this == obj)
        return true;

      if (getClass() != obj.getClass())
        return false;

      AsmWriterOperand otherOp = (AsmWriterOperand) obj;
      if (!opTy.equals(otherOp.opTy) || str.equals(otherOp.str))
        return false;

      if (opTy.equals(OperandType.isMachineInstrOperand))
        return (miOpNo != otherOp.miOpNo || !miModifier.equals(otherOp.miModifier));
      ;

      return false;
    }

    String emitCode() {
      if (opTy == isLiteralTextOperand) {
        if (str.length() == 1)
          return "os.print('" + str + "');";
        return "os.print(\"" + str + "\");";
      }
      if (opTy.equals(isLiteralStatementOperand))
        return str;
      StringBuilder sb = new StringBuilder();
      sb.append(str).append("(mi");
      if (miOpNo != ~0)
        sb.append(", ").append(miOpNo);
      if (miModifier != null && !miModifier.isEmpty())
        sb.append(", \"").append(miModifier).append("\"");

      return sb.append(");").toString();
    }
  }

  public static class AsmWriterInst {
    ArrayList<AsmWriterOperand> operands;

    CodeGenInstruction cgi;

    public AsmWriterInst(CodeGenInstruction cgi, Record asmWriter) {
      operands = new ArrayList<>();
      this.cgi = cgi;
      // ~0 if we are outside a {.|.|.} region, other #.
      int curVariant = ~0;
      int curColumn = 0;

      int variant = (int) asmWriter.getValueAsInt("Variant");
      int firstOperandColumn = (int) asmWriter.getValueAsInt("FirstOperandColumn");
      int operandSpacing = (int) asmWriter.getValueAsInt("OperandSpacing");
      String asmString = cgi.asmString.trim();

      //if (TableGen.DEBUG)
      //    System.out.println(asmString);

      int lastEmitted = 0;
      while (lastEmitted < asmString.length()) {
        // find the position of any character in the pattern.
        int dollarPos = Util.findFirstOf(asmString, "${|}\\", lastEmitted);
        if (dollarPos == -1)
          dollarPos = asmString.length();

        // emit a constant string fragment.
        if (dollarPos != lastEmitted) {
          if (curVariant == variant || curVariant == ~0) {
            for (; lastEmitted != dollarPos; ++lastEmitted) {
              switch (asmString.charAt(lastEmitted)) {
                case '\n':
                  addLiteralString("\\n");
                  break;
                case '\t':
                  if (firstOperandColumn == -1 || operandSpacing == -1)
                    addLiteralString("\\t");
                  else {
                    // We recognize a tab as an operand delimeter.
                    int destColumn = firstOperandColumn + curColumn++ * operandSpacing;
                    operands.add(new AsmWriterOperand("O.PadToColumn(" +
                        destColumn + ");\n",
                        isLiteralStatementOperand));
                  }
                  break;
                case '"':
                  addLiteralString("\\\"");
                  break;
                case '\\':
                  addLiteralString("\\\\");
                  break;
                default:
                  addLiteralString(String.valueOf(asmString.charAt(lastEmitted)));
                  ;
                  break;
              }
            }
          } else {
            lastEmitted = dollarPos;
          }
        } else if (asmString.charAt(dollarPos) == '\\') {
          if (dollarPos + 1 != asmString.length() &&
              (curVariant == variant || curVariant == ~0)) {
            if (asmString.charAt(dollarPos + 1) == 'n')
              addLiteralString("\\n");
            else if (asmString.charAt(dollarPos + 1) == 't') {
              if (firstOperandColumn == -1 || operandSpacing == -1) {
                addLiteralString("\\t");
                break;
              }

              int destColumn = firstOperandColumn + curColumn++ * operandSpacing;

              operands.add(new AsmWriterOperand("os.padToColumn(" +
                  destColumn + ");\n", isLiteralStatementOperand));
              break;
            } else if ("${|}\\".indexOf(asmString.charAt(dollarPos + 1)) != -1) {
              addLiteralString(String.valueOf(asmString.charAt(dollarPos + 1)));
            } else {
              Error.printFatalError("Non-supported escaped character found in instruction '" +
                  cgi.theDef.getName() + "'!");
            }
            lastEmitted = dollarPos + 2;
            continue;
          }
        } else if (asmString.charAt(dollarPos) == '{') {
          if (curVariant != ~0)
            Error.printFatalError("Nested variants found for instruction"
                + "'" + cgi.theDef.getName() + "'!");
          lastEmitted = dollarPos + 1;
          // We are now inside of the variant!
          curVariant = 0;
        } else if (asmString.charAt(dollarPos) == '|') {
          if (curVariant == ~0)
            Error.printFatalError("'|' character founds outside of "
                + "a variant in instruction '" + cgi.theDef.getName() + "'!");

          ++curVariant;
          ++lastEmitted;
        } else if (asmString.charAt(dollarPos) == '}') {
          if (curVariant == ~0)
            Error.printFatalError("'|' character founds outside of "
                + "a variant in instruction '" + cgi.theDef.getName() + "'!");
          ++lastEmitted;
          curVariant = ~0;
        } else if (dollarPos + 1 != asmString.length()
            && asmString.charAt(dollarPos + 1) == '$') {
          if (curVariant == variant || curVariant == ~0)
            addLiteralString("$");
          lastEmitted = dollarPos + 2;
        } else {
          // Get the namespace of the variable.
          int varEnd = dollarPos + 1;
          boolean hasCurlyBraces = false;
          if (varEnd < asmString.length() && asmString.charAt(varEnd) == '{') {
            hasCurlyBraces = true;
            ++dollarPos;
            ++varEnd;
          }
          while (varEnd < asmString.length() && isIdentPart(asmString.charAt(varEnd)))
            ++varEnd;

          String varName = asmString.substring(dollarPos + 1, varEnd);

          // Modifier - Support ${foo:modifier} syntax, where "modifier" is passed
          // into printOperand.
          String modifier = "";

          // In order to avoid starting the next string at the terminating curly
          // brace, advance the end position past it if we found an opening curly
          // brace.
          if (hasCurlyBraces) {
            if (varEnd >= asmString.length()) {
              Error.printFatalError("Reached end of string before "
                  + "terminating curly brace in '" + cgi.theDef
                  .getName() + "'");
            }
            // Look for a modifier string.
            if (asmString.charAt(varEnd) == ':') {
              ++varEnd;
              if (varEnd >= asmString.length()) {
                Error.printFatalError("Reached end of string before terminating curly brace in '"
                    + cgi.theDef.getName() + "'");
              }

              int modifierStart = varEnd;
              while (varEnd < asmString.length() && isIdentPart(
                  asmString.charAt(varEnd)))
                ++varEnd;

              modifier = asmString.substring(modifierStart, varEnd);

              if (modifier.isEmpty()) {
                Error.printFatalError("Bad operand modifier namespace in '" + cgi.theDef.getName() + "'");
              }
            }

            if (asmString.charAt(varEnd) != '}')
              Error.printFatalError("Variable namespace begining with '{' didn't end with '}' in "
                  + "' " + cgi.theDef.getName() + "'");

            ++varEnd;
          }

          if (varName.isEmpty() && modifier.isEmpty()) {
            Error.printFatalError("Stray '$' in '" + cgi.theDef.getName()
                + "' asm string, maybe you want $$?");
          }
          if (varName.isEmpty())
            operands.add(new AsmWriterOperand("PrintSpecial", ~0, modifier));
          else {
            int opNo = cgi.getOperandNamed(varName);
            CodeGenInstruction.OperandInfo opInfo = cgi.operandList.get(opNo);

            if (curVariant == variant || curVariant == ~0) {
              int miOp = opInfo.miOperandNo;
              operands.add(new AsmWriterOperand(opInfo.printerMethodName, miOp, modifier));
            }
          }
          lastEmitted = varEnd;
        }
      }
    }

    /**
     * If this instruction is exactly identical to the
     * specified instruction except for one differing operand, return the
     * differing operand number.  Otherwise return 0.
     *
     * @param other
     * @return
     */
    public int matchesAllButOneOp(AsmWriterInst other) {
      if (operands.size() != other.operands.size())
        return ~1;
      int mismatchOperand = ~0;
      for (int i = 0, e = operands.size(); i < e; i++) {
        if (!operands.get(i).equals(other.operands.get(i))) {
          if (mismatchOperand != ~0)
            mismatchOperand = ~1;
          else
            mismatchOperand = i;
        }
      }
      return mismatchOperand;
    }

    private void addLiteralString(String str) {
      if (!operands.isEmpty() && operands.get(operands.size() - 1).opTy
          == isLiteralTextOperand)
        operands.get(operands.size() - 1).str += str;
      else
        operands.add(new AsmWriterOperand(str));
    }
  }

  private ArrayList<CodeGenInstruction> numberedInstructions;
  private HashMap<CodeGenInstruction, AsmWriterInst> cgiMapToawi;

  /**
   * Outputs AsmWriter code, return true on failure.
   *
   * @throws Exception
   */
  @Override
  public void run(String outputFile) throws FileNotFoundException {
    numberedInstructions = new ArrayList<>();
    cgiMapToawi = new HashMap<>();

    if (outputFile.equals("-"))
      os = System.out;
    else
      os = new PrintStream(new FileOutputStream(outputFile));
    CodeGenTarget target = new CodeGenTarget(Record.records);
    String targetName = target.getName();
    String lowerTargetName = targetName.toLowerCase();

    os.println("package backend.target.x86;");
    os.println("import backend.mc.MCAsmInfo;");
    os.println("import backend.mc.MCInst;");
    os.println("import tools.Util;");
    os.println("import java.io.PrintStream;");

    emitSourceFileHeaderComment("Assemble Writer Source Fragment", os);
    Record asmWriter = target.getAsmWriter();

    String className = target.getName() + "Gen" +
        asmWriter.getValueAsString("AsmWriterClassName");
    String superName = target.getName() +
        asmWriter.getValueAsString("AsmWriterClassName");

    os.printf("public final class %s extends %s {\n\t", className, superName);

    emitPrintInstruction(os, target);
    emitGetRegisterName(os, target);
    emitGetInstructionName(os, target);

    // Emit constructor.
    os.printf("\n  public %s(PrintStream os, MCAsmInfo mai) {\n"
        + "    super(os, mai);\n  }\n", className);
    os.println("}");

    // Close the print stream.
    os.close();
  }

  private void emitPrintInstruction(PrintStream os, CodeGenTarget target) {
    Record asmWriter = target.getAsmWriter();

    String header = "/**\n"
        + "\t * This static array used for encoding asm and it's length of each instruction\n"
        + "\t * (including pseudo instr) as an element.\n"
        + "\t */\n";
    os.print(header);

    ArrayList<AsmWriterInst> instructions = new ArrayList<>();

    Collection<CodeGenInstruction> values = target.getInstructions().values();
    for (CodeGenInstruction inst : values) {
      String asmString = inst.asmString;
      if (!asmString.isEmpty())
        instructions.add(new AsmWriterInst(inst, asmWriter));
    }

    target.getInstructionsByEnumValue(numberedInstructions);

    // Compute the CodeGenInstruction -> AsmWriterInst mapping.  Note that not
    // all machine instructions are necessarily being printed, so there may be
    // target instructions not in this map.
    for (AsmWriterInst inst : instructions) {
      cgiMapToawi.put(inst.cgi, inst);
    }

    // Build an aggregate string, and build a table of offsets into it.
    TObjectIntHashMap<String> stringOffset = new TObjectIntHashMap<>();
    StringBuilder aggregateString = new StringBuilder();

    /// OpcodeInfo - This encodes the index of the string to use for the first
    /// chunk of the output as well as indices used for operand printing.
    ArrayList<Pair<Long, Integer>> opcodeInfo = new ArrayList<>();

    int maxStringIdx = 0;
    for (CodeGenInstruction inst : numberedInstructions) {
      AsmWriterInst awi = cgiMapToawi.get(inst);
      long idx;
      int len;
      if (!cgiMapToawi.containsKey(inst)) {
        cgiMapToawi.put(inst, null);
        idx = 0;
        len = 0;
      } else if (awi.operands.get(0).opTy != isLiteralTextOperand
          || awi.operands.get(0).str.isEmpty()) {
        idx = 1;
        len = 0;
      } else {
        String str = awi.operands.get(0).str;
        int entry = stringOffset.get(str);
        if (!stringOffset.containsKey(str)) {
          maxStringIdx = aggregateString.length();
          stringOffset.put(str, maxStringIdx);
          entry = maxStringIdx;
          aggregateString.append(str);
        }
        len = str.length();
        idx = entry;

        awi.operands.remove(0);
      }
      opcodeInfo.add(Pair.get(idx, len));
    }

    int asmStrBits = Util.log2Ceil(maxStringIdx + 1);

    /**
     * The first component of opcodeInfo is encoded as follow (from rightmost to leftmost):
     * 1. the index to asm string array.
     * 2. the index of first operand to command list
     *    (command means what action should be performed, like printi8MeM, printImm, printComment etc).
     * 3. the index of second operand to command list.
     * 4. the index of third operand to command list.
     *    ...
     */
    int bitsLeft = 64 - asmStrBits;

    ArrayList<ArrayList<String>> tableDrivenOperandPrinters = new ArrayList<>();
    TIntArrayList operandOffsets = new TIntArrayList();

    boolean isFirst = true;
    while (true) {
      ArrayList<String> uniqueOperandCommands = new ArrayList<>();

      // For the first operand check, add a default value for instructions with
      // just opcode strings to use.
      if (isFirst) {
        uniqueOperandCommands.add("\t\t\t\treturn;\n");
        isFirst = false;
      }

      // instIdx indicates the index of i'th element in numberedInstructions
      // to command list.
      TIntArrayList instIdxs = new TIntArrayList();
      // how many operands have been handled each time, it usual be 0 or 1.
      TIntArrayList numInstOpsHandled = new TIntArrayList();
      findUniqueOperandCommands(uniqueOperandCommands, instIdxs, numInstOpsHandled);

      if (uniqueOperandCommands.isEmpty())
        break;

      int numBits = Util.log2Ceil(uniqueOperandCommands.size());
      Util.assertion(bitsLeft >= numBits, "Not enough bits to densely encode %d more bits\n");
      operandOffsets.add(numBits);
      for (int i = 0, e = instIdxs.size(); i != e; i++) {
        if (instIdxs.get(i) != ~0) {
          opcodeInfo.get(i).first |= ((long) instIdxs.get(i)) << (64 - bitsLeft);
        }
      }
      bitsLeft -= numBits;

      // remove have been handled operands of each AsmWriterInst.
      for (int j = 0, sz = numberedInstructions.size(); j != sz; j++) {
        AsmWriterInst inst = getAsmWriterInstByID(j);
        if (inst != null) {
          if (!inst.operands.isEmpty()) {
            int numOps = numInstOpsHandled.get(instIdxs.get(j));
            Util.assertion(numOps <= inst.operands.size(), "Can't remove this many ops!");
            for (int t = 0; t != numOps; t++) {
              inst.operands.remove(t);
              t--;
              numOps--;
            }
          }
        }
      }

      // Remember the handlers for this set of operands.
      tableDrivenOperandPrinters.add(uniqueOperandCommands);
    }

    Util.assertion(operandOffsets.size() == tableDrivenOperandPrinters.size());

    os.printf("\tprivate static final long[][] opInfo = {\n");
    for (int i = 0, e = numberedInstructions.size(); i != e; i++) {
      os.printf("\t{0x%xL, %d},\t// %s\n",
          opcodeInfo.get(i).first,
          opcodeInfo.get(i).second,
          numberedInstructions.get(i).theDef.getName());
    }
    os.print("\t};\n");

    // Emit the string itself.
    os.printf("\tprivate static final String asmStrs = \n\t\t\"");
    escapeString(aggregateString);
    os.print(aggregateString);
    //escapeString(aggregateString);
    int charsPrinted = 0;
    for (int i = 0, e = aggregateString.length(); i != e; i++) {
      if (charsPrinted > 70) {
        os.print("\"\n\t\t\t+ \"");
        charsPrinted = 0;
      }
      os.print(aggregateString.charAt(i));
      ++charsPrinted;

      if (aggregateString.charAt(i) == '\\') {
        Util.assertion(i + 1 < aggregateString.length(), "Incomplete escape sequence!");
        if (Character.isDigit(aggregateString.charAt(i + 1))) {
          Util.assertion(Character.isDigit(aggregateString.charAt(i + 2)) &&
              Character.isDigit(aggregateString.charAt(i + 3)), "Expected 3 digit octal escape!");

          os.print(aggregateString.charAt(++i));
          os.print(aggregateString.charAt(++i));
          os.print(aggregateString.charAt(++i));
          charsPrinted += 3;
        } else {
          os.print(aggregateString.charAt(++i));
          ++charsPrinted;
        }
      }
    }

    os.print("\";\n\n");


    header = "\t/**\n " +
        "\t* This method is automatically generated by tablegen\n "
        + "\t* from the instruction set text. This method\n "
        + "\t* returns true if the machine instruction was sufficiently\n "
        + "\t* described to print it, otherwise it will return false.\n "
        + "\t*/\n";
    os.print(header);
    os.print("\t@Override\n\tpublic void printInst(MCInst mi) \n\t{\n");

    os.println("\t\t// Emit the opcode for the instruction.");
    os.println("\t\tos.print('\\t');");
    os.println("\t\tlong bits = opInfo[mi.getOpcode()][0];");
    os.println("\t\tUtil.assertion(bits != 0,  \"Cannot print this instruction\");\n");

    int asmStrBitsMask = (1 << asmStrBits) - 1;
    os.printf("\t\t// Starting index of asm namespace encoded into %d bit%n", asmStrBits);

    os.printf("\t\tint asmStrIdx = (int)bits&0x%x, len = (int)opInfo[mi.getOpcode()][1];\n",
        asmStrBitsMask);
    os.println("\t\tString asmName = asmStrs.substring(asmStrIdx, asmStrIdx + len);");
    os.println("\t\tStringBuilder buf = new StringBuilder();\n"
        + "\t\tfor (int i = 0, e = asmName.length(); i < e; i++)\n"
        + "        {\n"
        + "            if (asmName.charAt(i) == '\\\\' && i < e - 1)\n"
        + "            {\n"
        + "                switch (asmName.charAt(i+1))\n"
        + "                {\n" + "                    case 't':\n"
        + "                        buf.append('\\t');\n"
        + "                        break;\n"
        + "                    case 'n':\n"
        + "                        buf.append('\\n');\n"
        + "                        break;\n"
        + "                    case 'v':\n"
        + "                        buf.append(11);\n"
        + "                    case 'a':\n"
        + "                        buf.append(7);\n"
        + "                        break;\n"
        + "                    case 'f':\n"
        + "                        buf.append('\\f');\n"
        + "                        break;\n"
        + "                    case 'r':\n"
        + "                        buf.append('\\r');\n"
        + "                        break;\n"
        + "                    case '\\\\':\n"
        + "                        buf.append('\\\\');\n"
        + "                        break;\n"
        + "                    case '\\'':\n"
        + "                        buf.append('\\'');\n"
        + "                        break;\n"
        + "                    case '\"':\n"
        + "                        buf.append('\"');\n"
        + "                        break;\n"
        + "                    case '?':\n"
        + "                        buf.append('?');\n"
        + "                        break;\n"
        + "                    default:\n"
        + "                        buf.append('\\\\');\n"
        + "                        buf.append(asmName.charAt(i+1));\n"
        + "                        break;\n" + "                }\n"
        + "                i += 1;\n" + "                continue;\n"
        + "            }\n"
        + "            buf.append(asmName.charAt(i));\n" + "        }\n"
        + "        asmName = buf.toString();");

    os.println("\n\t\tos.print(asmName);");

    os.print("\t\tint number = 0;\n");
    os.print("\t\t// Each code fragment is just for each asm operand\n");

    int rightShiftAmt = asmStrBits;
    for (int i = 0, e = tableDrivenOperandPrinters.size(); i != e; i++) {
      ArrayList<String> commands = tableDrivenOperandPrinters.get(i);
      int numBits = operandOffsets.get(i);

      os.printf("\n\t\t// The %d's operands encoded into %d bit for %d unique commands.\n",
          i + 1, operandOffsets.get(i), commands.size());

      if (commands.size() == 2) {
        os.printf("\t\tnumber = (int)((bits >>> 0x%x) & 0x%x);\n",
            rightShiftAmt,
            (1 << numBits) - 1);

        os.printf("\t\tif (number != 0)\n\t\t{\n%s", commands.get(1));
        os.printf("\t\t}\n\t\telse\n\t\t{\n%s\t\t}", commands.get(0));
      } else {
        os.printf("\t\tnumber = (int)((bits >>> 0x%x) & 0x%x);\n",
            rightShiftAmt,
            (1 << numBits) - 1);
        os.printf("\t\tswitch(number) \n\t\t{\n");
        os.printf("\t\t\tdefault:\t// unreachable.\n");

        // Print out all the cases.
        for (int j = 0, sz = commands.size(); j < sz; j++) {
          os.printf("\t\t\tcase %d:\n", j);
          os.print(commands.get(j));
          if (!commands.get(j).trim().endsWith("return;"))
            os.print("\t\t\t\tbreak;\n");
        }
        os.print("\t\t}\n\n");
      }
      rightShiftAmt += numBits;
    }

    // Okay, delete instructions with no operand info left.
    for (int i = 0, e = instructions.size(); i != e; i++) {
      AsmWriterInst inst = instructions.get(i);
      if (inst.operands.isEmpty()) {
        instructions.remove(i);
        --i;
        --e;
      }
    }

    // Because this is a vector, we want to emit from the end.  Reverse all of the
    // elements in the vector.
    Collections.reverse(instructions);

    if (!instructions.isEmpty()) {
      os.print("\t\t\tswitch(mi.getOpcode()) {\n");
      while (!instructions.isEmpty()) {
        emitInstructions(instructions, os);
      }
      os.print("\t\t\t}\n");
      os.print("\t\treturn;\n");
    }
    os.print("\t}\n");
  }

  private void emitGetRegisterName(PrintStream os, CodeGenTarget target) {
    ArrayList<CodeGenRegister> registers = target.getRegisters();

    os.println();
    os.print("  private final static String[] RegNameStrs = {");
    for (int i = 0, e = registers.size(); i < e; i++) {
      CodeGenRegister reg = registers.get(i);
      String asmName = reg.theDef.getValueAsString("AsmName");
      if (asmName == null || asmName.isEmpty())
        asmName = reg.getName();

      if ((i % 10) == 0)
        os.print("\n    ");
      os.printf("\"%s\"", asmName);
      os.print(", ");
    }
    os.println("};\n");

    os.println("  public String getRegisterName(int regNo) {");
    os.printf("    Util.assertion(regNo > 0 && regNo < %d, \"Invalid register number!\");\n",
        registers.size()-1);
    os.println("    return RegNameStrs[regNo-1];");
    os.println("  }");
  }

  private void emitGetInstructionName(PrintStream os, CodeGenTarget target) {
    Record asmWriter = target.getAsmWriter();
    os.println();
    os.print("  private final static String[] InstNameStrs = {");
    for (int i = 0, e = numberedInstructions.size(); i < e; i++) {
      CodeGenInstruction cgi = numberedInstructions.get(i);
      String asmName = cgi.theDef.getName();

      if ((i % 6) == 0)
        os.print("\n    ");
      os.printf("\"%s\"", asmName);
      os.print(", ");
    }
    os.println("};\n");

    os.println("  public String getInstructionName(int opc) {");
    os.printf("    Util.assertion(opc < %d, \"Invalid instruction opcode\");\n",
        numberedInstructions.size());
    os.println("    return InstNameStrs[opc];");
    os.println("  }");
  }

  /**
   * Modify the argument string, turn '\\' and may non printable character into
   * escape character.
   *
   * @param str
   */
  private static void escapeString(StringBuilder str) {
    for (int i = 0; i != str.length(); i++) {
      if (str.charAt(i) == '\\') {
        ++i;
        str.insert(i, '\\');
      } else if (str.charAt(i) == '\t') {
        str.setCharAt(i++, '\\');
        str.insert(i, 't');
      } else if (str.charAt(i) == '"') {
        str.insert(i++, '\\');
      } else if (str.charAt(i) == '\n') {
        str.setCharAt(i++, '\\');
        str.insert(i, 'n');
      } else if (!TextUtils.isPrintable(str.charAt(i))) {
        // Always expand to a 3-digit octal escape.
        char ch = str.charAt(i);
        str.setCharAt(i++, '\\');
        str.insert(i++, '0' + ((ch / 64) & 7));
        str.insert(i++, '0' + ((ch / 8) & 7));
        str.insert(i, '0' + (ch & 7));
      }
    }
  }

  private AsmWriterInst getAsmWriterInstByID(int id) {
    Util.assertion(id < numberedInstructions.size());
    CodeGenInstruction inst = numberedInstructions.get(id);
    Util.assertion(cgiMapToawi.containsKey(inst), "Didn't find inst!");
    return cgiMapToawi.get(inst);
  }

  /**
   * <pre>
   * This method used for grouping some instructions into one set according to
   * the rule that the kind of first operand is same, for example, the first
   * operand are integral, so the action for the first operand of all above
   * instructions are same.
   * </pre>
   * <p>
   * Note that, this method called once, the first operand will be removed
   * from instruction operand list. So the first operand should be the second
   * one when next time called to this method.
   * </p>
   *
   * @param uniqueOperandCommands Each element of it represents the action
   *                              to be performed for printed out asm file in
   *                              corresponding case stmt.
   * @param instIdxs
   * @param instOpsUsed
   */
  private void findUniqueOperandCommands(
      ArrayList<String> uniqueOperandCommands,
      TIntArrayList instIdxs,
      TIntArrayList instOpsUsed) {
    instIdxs.fill(0, numberedInstructions.size(), ~0);

    // The size of instrsForCase is as same as uniqueOperandCommands.
    // Each element in instrsForCase indicates all instructions that
    // should be grouped into one be printed at one case stmt, because
    // the i'th operand of their is same of kind, e.g. all is printOperand(),
    // or printi8mem() etc.
    ArrayList<String> instrsForCase = new ArrayList<>();
    for (int i = uniqueOperandCommands.size(); i > 0; i--) {
      instrsForCase.add("");
      instOpsUsed.add(0);
    }

    for (int i = 0, e = numberedInstructions.size(); i != e; i++) {
      AsmWriterInst inst = getAsmWriterInstByID(i);
      if (inst == null)       // PHI, INLINEASM, DBG_LABEL, etc.
        continue;

      String command;
      // Once calling to findUniqueOperandCommand, the first operand will
      // be removed from operands list.
      if (inst.operands.isEmpty())
        continue;

      command = "\t\t\t\t" + inst.operands.get(0).getCode() + "\n";
      if (inst.operands.size() == 1)
        command += "\t\t\t\treturn;\n";

      boolean foundIt = false;
      for (int idx = 0, sz = uniqueOperandCommands.size(); idx != sz; ++idx) {
        if (uniqueOperandCommands.get(idx).equals(command)) {
          instIdxs.set(i, idx);
          instrsForCase.set(idx, instrsForCase.get(idx)
              + ", "
              + inst.cgi.theDef.getName());
          foundIt = true;
          break;
        }
      }

      if (!foundIt) {
        instIdxs.set(i, uniqueOperandCommands.size());
        uniqueOperandCommands.add(command);
        instrsForCase.add(inst.cgi.theDef.getName());

        instOpsUsed.add(1);
      }
    }

    for (int commandIdx = 0, e = uniqueOperandCommands.size(); commandIdx != e; commandIdx++) {
      for (int op = 1; ; ++op) {
        int nit = instIdxs.indexOf(commandIdx);
        if (nit < 0)
          break;
        AsmWriterInst firstInst = getAsmWriterInstByID(nit);
        if (firstInst == null || firstInst.operands.size() == op) {
          break;
        }

        boolean allSame = true;

        int maxSize = firstInst.operands.size();

        for (nit = instIdxs.indexOf(nit + 1, commandIdx);
             nit != -1;
             nit = instIdxs.indexOf(nit + 1, commandIdx)) {
          AsmWriterInst otherInst = getAsmWriterInstByID(nit);

          if (otherInst != null && otherInst.operands.size() > firstInst.operands.size())
            maxSize = Math.max(maxSize, otherInst.operands.size());

          if (otherInst == null || otherInst.operands.size() == op ||
              !otherInst.operands.get(op).equals(firstInst.operands.get(op))) {
            allSame = false;
            break;
          }
        }

        if (!allSame)
          break;

        String command = "\t\t\t\t" + firstInst.operands.get(op).getCode() + "\n";
        if (firstInst.operands.size() == op + 1 &&
            firstInst.operands.size() == maxSize) {
          command += "\t\t\t\treturn;\n";
        }

        uniqueOperandCommands.set(commandIdx, uniqueOperandCommands.get(commandIdx) + command);
        instOpsUsed.set(commandIdx, instOpsUsed.get(commandIdx) + 1);
      }
    }

    for (int i = 0, e = instrsForCase.size(); i != e; i++) {
      String insts = instrsForCase.get(i);
      if (insts.length() > 60) {
        // Truncate the string sequence consists of namespace of asm instructions
        // to 60 when its length exceed 80.
        insts = insts.substring(0, 60);
        insts += "...";
      }

      if (!insts.isEmpty()) {
        // Prepend '//' to the asm instruction string for printed out
        // as comment.
        uniqueOperandCommands.set(i, "\t\t\t\t// " + insts
            + "\n" + uniqueOperandCommands.get(i));
      }
    }
  }

  /**
   * emit the last instruction in the vector and any other
   * instructions that are suitably similar to it.
   *
   * @param insts
   * @param os
   */
  private static void emitInstructions(ArrayList<AsmWriterInst> insts,
                                       PrintStream os) {
    ListIterator<AsmWriterInst> itr = insts.listIterator(insts.size());
    AsmWriterInst firstInst = itr.previous();
    itr.remove();

    LinkedList<AsmWriterInst> similarInsts = new LinkedList<>();
    int differingOperand = ~0;
    while (itr.hasPrevious()) {
      AsmWriterInst curInstr = itr.previous();
      int diffOp = curInstr.matchesAllButOneOp(firstInst);
      if (diffOp != ~1) {
        if (differingOperand == ~0)  // First match!
          differingOperand = diffOp;

        // If this differs in the same operand as the rest of the instructions in
        // this class, move it to the similarInsts list.
        if (differingOperand == diffOp || diffOp == ~0) {
          similarInsts.add(curInstr);
          itr.remove();
        }
      }
    }

    os.print("\t\t\tcase " + firstInst.cgi.theDef.getName() + ":\n");
    for (int i = 0, e = similarInsts.size(); i != e; ++i)
      os.print("\t\t\tcase " + similarInsts.get(i).cgi.theDef.getName() + ":\n");
    for (int i = 0, e = firstInst.operands.size(); i != e; ++i) {
      if (i != differingOperand) {
        // If the operand is the same for all instructions, just print it.
        os.print("\t");
        os.printf("\t\t\t%s", firstInst.operands.get(i).emitCode());
      } else {
        // If this is the operand that varies between all of the instructions,
        // emit a switch for just this operand now.
        os.print("\t\tswitch (mi.getOpcode()) {\n");
        LinkedList<Pair<String, AsmWriterOperand>> opsToPrint = new LinkedList<>();
        opsToPrint.add(Pair.get(firstInst.cgi.theDef.getName(),
            firstInst.operands.get(i)));

        final int j = i;
        similarInsts.forEach(awi ->
        {
          opsToPrint.add(Pair.get(
              awi.cgi.theDef.getName(),
              awi.operands.get(j)));
        });

        Collections.reverse(opsToPrint);

        while (!opsToPrint.isEmpty())
          printCases(opsToPrint, os);
        os.print("\t}");
      }
      os.println();
    }
    os.println("\t\t\t\tbreak;");
  }

  private static void printCases(
      LinkedList<Pair<String, AsmWriterOperand>> opsToPrint,
      PrintStream os) {
    Pair<String, AsmWriterOperand> lastOne = opsToPrint.removeLast();
    os.printf("\tcase %s: ", lastOne.first);
    AsmWriterOperand operand = lastOne.second;

    ListIterator<Pair<String, AsmWriterOperand>> itr = opsToPrint.listIterator();
    while (itr.hasPrevious()) {
      lastOne = itr.previous();
      if (lastOne.second.equals(operand)) {
        os.printf("\n\tcase %s: ", lastOne.first);
      }
    }
    os.printf("\t\t\t\t%s", operand.emitCode());
    os.print("break;\n");
  }
}
