package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2016, Xlous
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

import tools.Pair;
import tools.Util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import static utils.tablegen.AsmWriterEmitter.AsmWriterOperand.OperandType.isLiteralStatementOperand;
import static utils.tablegen.AsmWriterEmitter.AsmWriterOperand.OperandType.isLiteralTextOperand;
import static utils.tablegen.AsmWriterEmitter.AsmWriterOperand.OperandType.isMachineInstrOperand;

/**
 * This class defined for generating partial code of AsmWriter, like Intel or
 * AT&T grammar.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class AsmWriterEmitter extends TableGenBackend
{
    private PrintStream os;

    private static boolean isIdentPart(char ch)
    {
        return ch >= 'a' && ch <= 'z'
                || ch >= 'A' && ch <= 'Z'
                || ch >= '0' && ch <= '9'
                || ch == '_';
    }

    static class AsmWriterOperand
    {
        enum OperandType
        {
            isLiteralTextOperand,
            isMachineInstrOperand,
            isLiteralStatementOperand;
        }

        String str;

        int miOpNo;

        String miModifier;

        OperandType opTy;

        AsmWriterOperand(String litStr)
        {
            this(litStr, isLiteralTextOperand);
        }

        public AsmWriterOperand(String litStr, OperandType opTy)
        {
            str = litStr;
            this.opTy = opTy;
        }

        AsmWriterOperand(String printer, int opNo, String modifier, OperandType opTy)
        {
            this.opTy = opTy;
            str = printer;
            miModifier = modifier;
            miOpNo = opNo;
        }
        AsmWriterOperand(String printer, int opNo, String modifier)
        {
            this(printer, opNo, modifier, isMachineInstrOperand);
        }

        @Override
        public boolean equals(Object obj)
        {
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
                return (miOpNo != otherOp.miOpNo || !miModifier.equals(otherOp.miModifier));;

                return false;
        }

        String emitCode()
        {
            if (opTy == isLiteralTextOperand)
            {
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

    public static class AsmWriterInst
    {
        ArrayList<AsmWriterOperand> operands;

        CodeGenInstruction cgi;

        public AsmWriterInst(CodeGenInstruction cgi, Record asmWriter) throws Exception
        {
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
            while (lastEmitted < asmString.length())
            {
                // find the position of any character in the pattern.
                int dollarPos = Util.findFirstOf(asmString, "${|}\\", lastEmitted);
                if (dollarPos == -1)
                    dollarPos = asmString.length();

                // emit a constant string fragment.
                if (dollarPos != lastEmitted)
                {
                    if (curVariant == variant || curVariant == ~0)
                    {
                        for (; lastEmitted != dollarPos; ++lastEmitted)
                        {
                            switch (asmString.charAt(lastEmitted))
                            {
                                case '\n':
                                    addLiteralString("\\n");
                                    break;
                                case '\t':
                                    if (firstOperandColumn == - 1 || operandSpacing == -1)
                                        addLiteralString("\\t");
                                    else
                                    {
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
                                    addLiteralString(String.valueOf(asmString.charAt(lastEmitted)));;
                                    break;
                            }
                        }
                    }
                    else
                    {
                        lastEmitted = dollarPos;
                    }
                }
                else if (asmString.charAt(dollarPos) == '\\')
                {
                    if (dollarPos + 1 != asmString.length() &&
                            (curVariant == variant || curVariant == ~0))
                    {
                        if (asmString.charAt(dollarPos + 1) == 'n')
                            addLiteralString("\\n");
                        else if (asmString.charAt(dollarPos + 1) == 't')
                        {
                            if (firstOperandColumn == -1 || operandSpacing == -1)
                            {
                                addLiteralString("\\t");
                                break;
                            }

                            int destColumn = firstOperandColumn + curColumn++ * operandSpacing;

                            operands.add(new AsmWriterOperand("O.PadToColumn(" +
                                destColumn + "):\n", isLiteralStatementOperand));
                            break;
                        }
                        else if ("${|}\\".indexOf(asmString.charAt(dollarPos + 1)) != -1)
                        {
                            addLiteralString(String.valueOf(asmString.charAt(dollarPos + 1)));
                        }
                        else
                        {
                            throw new Exception("Non-supported escaped character found in instruction '" +
                                cgi.theDef.getName() + "'!");
                        }
                        lastEmitted = dollarPos + 2;
                        continue;
                    }
                }
                else if (asmString.charAt(dollarPos) == '{')
                {
                    if (curVariant != ~0)
                        throw new Exception("Nested variants found for instruction"
                            + "'" + cgi.theDef.getName() + "'!");
                    lastEmitted = dollarPos + 1;
                    // We are now inside of the variant!
                    curVariant = 0;
                }
                else if (asmString.charAt(dollarPos) == '|')
                {
                    if (curVariant == ~0)
                        throw new Exception("'|' character founds outside of "
                                + "a variant in instruction '" + cgi.theDef.getName() + "'!");

                    ++curVariant;
                    ++lastEmitted;
                }
                else if (asmString.charAt(dollarPos) == '}')
                {
                    if (curVariant == ~0)
                        throw new Exception("'|' character founds outside of "
                                + "a variant in instruction '" + cgi.theDef.getName() + "'!");
                    ++lastEmitted;
                    curVariant = ~0;
                }
                else if (dollarPos + 1 != asmString.length()
                        && asmString.charAt(dollarPos+1) == '$')
                {
                    if (curVariant == variant || curVariant == ~0)
                        addLiteralString("$");
                    lastEmitted = dollarPos + 2;
                }
                else
                {
                    // Get the name of the variable.
                    int varEnd = dollarPos + 1;
                    boolean hasCurlyBraces = false;
                    if (varEnd < asmString.length() && asmString.charAt(varEnd) == '{')
                    {
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
                    if (hasCurlyBraces)
                    {
                        if (varEnd >= asmString.length())
                        {
                            throw new Exception("Reached end of string before "
                                    + "terminating curly brace in '" + cgi.theDef
                                    .getName() + "'");
                        }
                        // Look for a modifier string.
                        if (asmString.charAt(varEnd) == ':')
                        {
                            ++varEnd;
                            if (varEnd >= asmString.length())
                            {
                                throw new Exception(
                                        "Reached end of string before terminating curly brace in '"
                                                + cgi.theDef.getName() + "'");
                            }

                            int modifierStart = varEnd;
                            while (varEnd < asmString.length() && isIdentPart(
                                    asmString.charAt(varEnd)))
                                ++varEnd;

                            modifier = asmString.substring(modifierStart, varEnd);

                            if (modifier.isEmpty())
                            {
                                throw new Exception(
                                        "Bad operand modifier name in '" + cgi.theDef.getName() + "'");
                            }
                        }

                        if (asmString.charAt(varEnd) != '}')
                            throw new Exception(
                                    "Variable name begining with '{' didn't end with '}' in "
                                            + "' " + cgi.theDef.getName() + "'");

                        ++varEnd;
                    }

                    if (varName.isEmpty() && modifier.isEmpty())
                    {
                        throw new Exception("Stray '$' in '" + cgi.theDef.getName()
                                + "' asm string, maybe you want $$?");
                    }
                    if (varName.isEmpty())
                        operands.add(new AsmWriterOperand("PrintSpecial", ~0, modifier));
                    else
                    {
                        int opNo = cgi.getOperandNamed(varName);
                        CodeGenInstruction.OperandInfo opInfo = cgi.operandList.get(opNo);

                        if (curVariant == variant || curVariant == ~0)
                        {
                            int miOp = opInfo.miOperandNo;
                            operands.add(new AsmWriterOperand(opInfo.printerMethodName, miOp, modifier));
                        }
                    }
                    lastEmitted = varEnd;
                }
            }

            operands.add(new AsmWriterOperand("emitComments(mi);\n", isLiteralStatementOperand));
            addLiteralString("\\n");
        }

        /**
         * If this instruction is exactly identical to the
         * specified instruction except for one differing operand, return the
         * differing operand number.  Otherwise return 0.
         * @param other
         * @return
         */
        public int matchesAllButOneOp(AsmWriterInst other)
        {
            if (operands.size() != other.operands.size())
                return ~1;
            int mismatchOperand = ~0;
            for (int i = 0, e = operands.size(); i < e; i++)
            {
                if (!operands.get(i).equals(other.operands.get(i)))
                {
                    if (mismatchOperand != ~0)
                        mismatchOperand = ~1;
                    else
                        mismatchOperand = i;
                }
            }
            return mismatchOperand;
        }

        private void addLiteralString(String str)
        {
            if (!operands.isEmpty() && operands.get(operands.size() - 1).opTy
                    == isLiteralTextOperand)
                operands.get(operands.size() - 1).str += str;
            else
                operands.add(new AsmWriterOperand(str));
        }
    }

    /**
     * Outputs AsmWriter code, return true on failure.
     * @throws Exception
     */
    @Override
    public void run(String outputFile) throws Exception
    {
        if (outputFile.equals("-"))
            os = System.out;
        else
            os = new PrintStream(new FileOutputStream(outputFile));

        emitSourceFileHeaderComment("Assembly Writer Source Fragment", os);
        CodeGenTarget target = new CodeGenTarget();
        Record asmWriter = target.getAsmWriter();

        String className = target.getName() + "Gen" +
                asmWriter.getValueAsString("AsmWriterClassName");
        String superName = target.getName() + asmWriter.getValueAsString("AsmWriterClassName");

        long variant = asmWriter.getValueAsInt("Variant");

        os.printf("public final class %s extends %s{\n\t", className, superName);

        String header = "/**\n "+
                         "* This method is automatically generated by tablegen\n "
                         + "* from the instruction set description. This method\n "
                         + "* returns true if the machine instruction was sufficiently\n "
                         + "* described to print it, otherwise it will return false.\n "
                         + "*/\n";

        os.print(header);

        ArrayList<AsmWriterInst> instructions = new ArrayList<>();

        Collection<CodeGenInstruction> values = target.getInstructions().values();
        for (CodeGenInstruction inst : values)
        {
            String asmString = inst.asmString;
            if (!asmString.isEmpty())
                instructions.add(new AsmWriterInst(inst, asmWriter));
        }

        // If all of the instructions start with a constant string (a very very common
        // occurrence), emit all of the constant strings as a big table lookup instead
        // of requiring a switch for them.
        boolean allStartWithString = true;

        for (AsmWriterInst inst : instructions)
        {
            if (inst.operands.isEmpty() || inst.operands.get(0).opTy
                    != isLiteralTextOperand)
            {
                allStartWithString = false;
                break;
            }
        }

        ArrayList<CodeGenInstruction> numberedInstructions = new ArrayList<>();

        target.getInstructionsByEnumValue(numberedInstructions);

        if (allStartWithString)
        {
            // Compute the CodeGenInstruction -> AsmWriterInst mapping.  Note that not
            // all machine instructions are necessarily being printed, so there may be
            // target instructions not in this map.
            HashMap<CodeGenInstruction, AsmWriterInst> cgiMapToawi = new HashMap<>();
            instructions.forEach(inst-> cgiMapToawi.put(inst.cgi, inst));

            // emit a table of constant strings.
            os.print("\tpublic static final String[] opStrs = {\n");
            numberedInstructions.forEach(cgi->
            {
                if (!cgiMapToawi.containsKey(cgi))
                    os.print("\t\t\tnull,\t\t\t\t// ");
                else
                {
                    AsmWriterInst awi = cgiMapToawi.get(cgi);
                    os.printf("\t\t\t\"%s\",\t\t\t\t// ", awi.operands.get(0).str);
                }
                os.println(cgi.theDef.getName());
            });

            os.print("\t};\n\n");
        }

        os.print("\t@Override\n\tpublic boolean printInstruction(MachineOperation mi) \n\t{\n");

        os.println("\t\t// emit the opcode for the instruction.");
        os.println("\t\tString asmStr = opStrs[mi.getOpcode()]");
        os.println("\t\tif (asmStr != null)\n");
        os.print("\t\t\tos.print(asmStr);\n\n");

        // Because this is a vector we want to emit from the end.  Reverse all of the
        // elements in the vector.
        Collections.reverse(instructions);

        os.print("\t\tswitch (mi.getOpcode()) \n{\n");
        os.print("\t\t\tdefault: return false;\n");
        os.print("\t\t\tcase INLINEASM: printInlineAsm(mi); break;\n");

        while (!instructions.isEmpty())
            emitInstructions(instructions, os);

        os.print("\t\t\t}// end of switch.\n");
        os.print("\t\treturn true;\n");
        os.print("\t}// end of printInstruction.\n");
        os.print("}// end of interface.\n");

        // Close the print stream.
        os.close();
    }

    /**
     * emit the last instruction in the vector and any other
     * instructions that are suitably similar to it.
     * @param insts
     * @param os
     */
    private static void emitInstructions(ArrayList<AsmWriterInst> insts,
            PrintStream os)
    {
        ListIterator<AsmWriterInst> itr = insts.listIterator(insts.size());
        AsmWriterInst firstInst = itr.previous();
        itr.remove();

        LinkedList<AsmWriterInst> similarInsts = new LinkedList<>();
        int differingOperand = ~0;
        while (itr.hasPrevious())
        {
            AsmWriterInst curInstr = itr.previous();
            int diffOp = curInstr.matchesAllButOneOp(firstInst);
            if (diffOp != ~1)
            {
                if (differingOperand == ~0)  // First match!
                    differingOperand = diffOp;

                // If this differs in the same operand as the rest of the instructions in
                // this class, move it to the similarInsts list.
                if (differingOperand == diffOp || diffOp == ~0)
                {
                    similarInsts.add(curInstr);
                    itr.remove();
                }
            }
        }
        
        os.print("\t\t\tcase " + firstInst.cgi.theDef.getName() + ":\n");
        for (int i = 0, e = similarInsts.size(); i != e; ++i)
            os.print("\t\t\tcase " + similarInsts.get(i).cgi.theDef.getName() + ":\n");
        for (int i = 0, e = firstInst.operands.size(); i != e; ++i)
        {
            if (i != differingOperand)
            {
                // If the operand is the same for all instructions, just print it.
                os.print("\t");
                os.printf("\t\t\t%s", firstInst.operands.get(i).emitCode());
            }
            else
            {
                // If this is the operand that varies between all of the instructions,
                // emit a switch for just this operand now.
                os.print("\t\tswitch (mi.getOpcode()) {\n");
                LinkedList<Pair<String, AsmWriterOperand>> opsToPrint  = new LinkedList<>();
                opsToPrint.add(Pair.get(firstInst.cgi.theDef.getName(),
                        firstInst.operands.get(i)));

                final int j = i;
                similarInsts.forEach(awi->
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
            PrintStream os)
    {
        Pair<String, AsmWriterOperand> lastOne = opsToPrint.removeLast();
        os.printf("\tcase %s: ", lastOne.first);
        AsmWriterOperand operand = lastOne.second;

        ListIterator<Pair<String, AsmWriterOperand>> itr = opsToPrint.listIterator();
        while (itr.hasPrevious())
        {
            lastOne = itr.previous();
            if (lastOne.second.equals(operand))
            {
                os.printf("\n\tcase %s: ", lastOne.first);
            }
        }
        os.printf("\t\t\t\t%s", operand.emitCode());
        os.print("break;\n");
    }
}
