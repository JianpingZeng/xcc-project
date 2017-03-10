package tools.tablegen;
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

import java.io.PrintStream;
import java.util.*;

import static tools.tablegen.AsmWriterEmitter.AsmWriterOperand.OperandType.isLiteralTextOperand;

/**
 * This class defined for generating partial code of AsmWriter, like Intel or
 * AT&T grammar.
 * @author Xlous.zeng
 * @version 0.1
 */
public final class AsmWriterEmitter extends TableGenBackend
{
    private PrintStream os;
    private String path;

    AsmWriterEmitter(String path)
    {
        this.path = path;
    }

    private static boolean isLetter(char ch)
    {
        return ch >= 'a' && ch <= 'z'
                || ch >= 'A' && ch <= 'Z'
                || ch == '_';
    }

    static class AsmWriterOperand
    {
        enum OperandType { isLiteralTextOperand, isMachineInstrOperand}

        String str;

        int miOpNo;

        String miModifier;

        OperandType opTy;

        AsmWriterOperand(String litStr)
        {
            opTy = isLiteralTextOperand;
            str = litStr;
        }

        AsmWriterOperand(String printer, int opNo, String modifier)
        {
            opTy = OperandType.isMachineInstrOperand;
            str = printer;
            miModifier = modifier;
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
            if (opTy != otherOp.opTy || !Objects.equals(str, otherOp.str))
                return false;

            return opTy == OperandType.isMachineInstrOperand && (
                    miOpNo != otherOp.miOpNo || !Objects
                            .equals(miModifier, otherOp.miModifier));
        }

        void emitCode(PrintStream os)
        {
            if (opTy == isLiteralTextOperand)
                os.print("os.print(\"" + str + "\");");
            else
            {
                os.print(str);
                os.print("(mi, "+ miOpNo);
                if (!miModifier.isEmpty())
                    os.printf(", \"%s\"", miModifier);
                os.print(");");
            }
        }
    }

    static class AsmWriterInst
    {
        ArrayList<AsmWriterOperand> operands;

        CodeGenInstruction cgi;

        AsmWriterInst(CodeGenInstruction cgi, int varint) throws Exception
        {
            this.cgi = cgi;
            // ~0 if we are outside a {.|.|.} region, other #.
            int curVariant = ~0;
            String asmString = cgi.asmString;

            int lastEmitted = 0;
            while (lastEmitted < asmString.length())
            {
                // find the position of any character in the pattern.
                int dollarPos = asmString.indexOf("[$ | \\| { | }]", lastEmitted);
                if (dollarPos >= asmString.length())
                    dollarPos = asmString.length();

                // Emit a constant string fragment.
                if (dollarPos != lastEmitted)
                {
                    if (curVariant == varint || curVariant == ~0)
                        addLiteralString(asmString.substring(lastEmitted, dollarPos));;

                    lastEmitted = dollarPos;
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
                    if (curVariant == varint || curVariant == ~0)
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
                    while (varEnd < asmString.length() && isLetter(asmString.charAt(varEnd)))
                        ++varEnd;

                    String varName = asmString.substring(dollarPos + 1, varEnd);

                    // Modifier - Support ${foo:modifier} syntax, where "modifier" is passed
                    // into printOperand.
                    String modifier = "";

                    if (hasCurlyBraces)
                    {
                        if (varEnd >= asmString.length())
                        {
                            throw new Exception("Reached end of string before t"
                                    + "erminating curly brace in '" + cgi.theDef
                                    .getName() + "'");
                        }
                        // Look for a modifier string.
                        if (asmString.charAt(varEnd) == ':')
                        {
                            ++varEnd;
                            if (varEnd >= asmString.length())
                            {
                                throw new Exception(
                                        "Reached end of string before t" + "erminating curly brace in '"
                                                + cgi.theDef.getName() + "'");
                            }

                            int modifierStart = varEnd;
                            while (varEnd < asmString.length() && isLetter(
                                    asmString.charAt(varEnd)))
                                ++varEnd;

                            modifier = asmString.substring(modifierStart, varEnd);

                            if (modifier.isEmpty())
                            {
                                throw new Exception(
                                        "Bad operand modifier name in '" + cgi.theDef.getName() + "'");
                            }

                            if (asmString.charAt(varEnd) != '}')
                                throw new Exception(
                                        "Variable name begining with '{' didn't end with '}' in "
                                                + "' " + cgi.theDef.getName() + "'");

                            ++varEnd;
                        }
                        if (varName.isEmpty())
                        {
                            throw new Exception("Stray '$' in '" + cgi.theDef.getName()
                                    + "' asm string, maybe you want $$?");
                        }

                        int opNo = cgi.getOperandNamed(varName);

                        CodeGenInstruction.OperandInfo opInfo = cgi.operandList.get(opNo);

                        int miOp = opInfo.miOperandNo;
                        // If this is a two-address instruction and we are not accessing the
                        // 0th operand, remove an operand.
                        if (cgi.isTwoAddress && miOp != 0)
                        {
                            if (miOp == 1)
                                throw new Exception("Should refer to #0 operand instead "
                                        + "of #1 for two-address instruction '"
                                        + cgi.theDef.getName() +"'");
                            --miOp;
                        }

                        if (curVariant == varEnd || curVariant == ~0)
                        {
                            operands.add(new AsmWriterOperand(opInfo.printerMethodName,
                                    miOp, modifier));
                        }
                        lastEmitted = varEnd;
                    }
                }
            }
            addLiteralString("\\n");
        }

        /**
         * If this instruction is exactly identical to the
         * specified instruction except for one differing operand, return the
         * differing operand number.  Otherwise return 0.
         * @param other
         * @return
         */
        int matchesAllButOneOp(AsmWriterInst other)
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
            AsmWriterOperand lastOperand = operands.get(operands.size() - 1);
            if (!operands.isEmpty() && lastOperand.opTy
                    == isLiteralTextOperand)
                lastOperand.str += str;
            else
                operands.add(new AsmWriterOperand(str));
        }
    }

    /**
     * Outputs AsmWriter code, return true on failure.
     * @throws Exception
     */
    @Override
    public void run() throws Exception
    {
        emitSourceFileHeaderComment("Assembly Writer Source Fragment", os);
        CodeGenTarget target = new CodeGenTarget();
        Record asmWriter = target.getAsmWriter();

        String className = target.getName() + "Gen" +
                asmWriter.getValueAsString("AsmWriterClassName");
        String superName = target.getName() + asmWriter.getValueAsString("AsmWriterClassName");

        os = new PrintStream(path + className +".java");

        int variant = asmWriter.getValueAsInt("Variant");

        os.printf("public final class %s extends %s{\n\t", className, superName);

        String header = "/**\n "+
                         "* This method is automatically generated by tablegen\n "
                         + "* from the instruction set description. This method\n "
                         + "* returns true if the machine instruction was sufficiently\n "
                         + "* described to print it, otherwise it will return false.\n "
                         + "*/\n";

        os.print(header);

        ArrayList<AsmWriterInst> instructions = new ArrayList<>();

        for (CodeGenInstruction inst : target.getInstructions().values())
        {
            String asmString = inst.asmString;
            if (!asmString.isEmpty())
                instructions.add(new AsmWriterInst(inst, variant));
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

        LinkedList<CodeGenInstruction> numberedInstructions = new LinkedList<>();

        target.getInstructionsByEnumValue(numberedInstructions);

        if (allStartWithString)
        {
            // Compute the CodeGenInstruction -> AsmWriterInst mapping.  Note that not
            // all machine instructions are necessarily being printed, so there may be
            // target instructions not in this map.
            HashMap<CodeGenInstruction, AsmWriterInst> cgiMapToawi = new HashMap<>();
            instructions.forEach(inst-> cgiMapToawi.put(inst.cgi, inst));

            // Emit a table of constant strings.
            os.print("  public static final String[] opStrs = {\n");
            numberedInstructions.forEach(cgi->
            {
                if (!cgiMapToawi.containsKey(cgi))
                    os.print("\tnull,\t//");
                else
                {
                    AsmWriterInst awi = cgiMapToawi.get(cgi);
                    os.printf("\t\"%s \",\t//", awi.operands.get(0).str);
                }
                os.println(cgi.theDef.getName());
            });

            os.print("};\n");
        }

        os.print("\t@Override \n\tprotected boolean printInstruction(MachineOperation mi) \n{\n");

        os.println("\t// Emit the opcode for the instruction.");
        os.println("\tString asmStr = opStrs[mi.getOpcode()]");
        os.println("\tif (asmStr != null)");
        os.print("\t\tos.print(asmStr);\n\n");

        // Because this is a vector we want to emit from the end.  Reverse all of the
        // elements in the vector.
        Collections.reverse(instructions);

        os.print("\t switch (mi.getOpcode()) {\n");
        os.print("\tdefault; return false;\n");
        os.print("\tcase INLINEASM: printInlineAsm(mi); break;\n");

        while (!instructions.isEmpty())
            emitInstructions(instructions, os);

        os.print("\t}// end of switch.\n");
        os.print("\treturn true;\n");
        os.print("}// end of printInstruction.\n");
        os.print("}// end of interface.\n");

        // Close the print stream.
        os.close();
    }

    /**
     * Emit the last instruction in the vector and any other
     * instructions that are suitably similar to it.
     * @param insts
     * @param os
     */
    private static void emitInstructions(ArrayList<AsmWriterInst> insts,
            PrintStream os)
    {
        int len = insts.size();
        AsmWriterInst firstInst = insts.get(len - 1);
        insts.remove(len - 1);

        LinkedList<AsmWriterInst> similarInsts = new LinkedList<>();
        int differingOperand = ~0;
        for (int i = insts.size(); i != 0; --i)
        {
            int diffOp = insts.get(i-1).matchesAllButOneOp(firstInst);
            if (diffOp != ~1)
            {
                if (differingOperand == ~0)  // First match!
                    differingOperand = diffOp;

                // If this differs in the same operand as the rest of the instructions in
                // this class, move it to the similarInsts list.
                if (differingOperand == diffOp || diffOp == ~0)
                {
                    similarInsts.add(insts.get(i - 1));
                    insts.remove(i - 1);
                }
            }
        }
        
        os.print("  case " + firstInst.cgi.theDef.getName() + ":\n");
        for (int i = 0, e = similarInsts.size(); i != e; ++i)
            os.print("  case " + similarInsts.get(i).cgi.theDef.getName() + ":\n");
        for (int i = 0, e = firstInst.operands.size(); i != e; ++i)
        {
            if (i != differingOperand)
            {
                // If the operand is the same for all instructions, just print it.
                os.print("\t");
                firstInst.operands.get(i).emitCode(os);
            }
            else
            {
                // If this is the operand that varies between all of the instructions,
                // emit a switch for just this operand now.
                os.print("    switch (MI->getOpcode()) {\n");
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
        os.println("\tbreak;");
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
        operand.emitCode(os);
        os.print("break;\n");
    }
}
