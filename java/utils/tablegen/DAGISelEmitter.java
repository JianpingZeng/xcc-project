/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous
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

package utils.tablegen;

import backend.codegen.MVT;
import tools.OutParamWrapper;
import tools.Pair;
import tools.Util;
import utils.tablegen.Init.IntInit;
import utils.tablegen.PatternCodeEmitter.*;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

import static backend.codegen.MVT.getEnumName;
import static utils.tablegen.PatternCodeEmitter.GeneratedCodeKind.*;
import static utils.tablegen.PatternCodeEmitter.GeneratedCodeKind.Init;
import static utils.tablegen.PatternCodeEmitter.*;

public class DAGISelEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private CodeGenDAGPatterns cgp;

    public DAGISelEmitter(RecordKeeper rec) throws Exception
    {
        records = rec;
        cgp = new CodeGenDAGPatterns(records);
    }

    private static String stripOutNewLineInEnding(String str)
    {
        if (str == null || str.isEmpty()) return "";
        int i = 0, len = str.length(), j = str.length() - 1;
        for (; i < len && str.charAt(i) == '\n'; i++);
        for (; j >= 0 && str.charAt(j) == '\n'; j--);
        if (i > j) return "";
        return str.substring(i, j+1);
    }

    private void emitHeader(PrintStream os, String targetName)
    {
        emitSourceFileHeaderComment("Instruction Selection based on DAG Covering for " + targetName+".", os);
        String ident = Util.fixedLengthString(4, ' ');
        os.printf("public final class %sGenDAGToDAGISel extends %sDAGToDAGISel%n{%n", targetName, targetName);

        os.printf("%spublic X86GenDAGToDAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt optLevel)%n" +
                "%s{%n%s%ssuper(tm, optLevel);%n%s", ident, ident, ident, ident, ident);
        os.println("}");
    }

    private void emitNodeTransform(PrintStream os)
    {
        // Sort the NodeTransform by name in  alphabetic order.
        TreeMap<String, Pair<Record, String>> nodesByName = new TreeMap<>();
        for (Map.Entry<Record, Pair<Record, String>> itr : cgp.getSdNodeXForms().entrySet())
        {
            nodesByName.put(itr.getKey().getName(), itr.getValue());
        }
        String ident = Util.fixedLengthString(2, ' ');

        os.printf("%s// Node transformation functions.%n", ident);
        for (Map.Entry<String, Pair<Record, String>> itr : nodesByName.entrySet())
        {
            Record node = itr.getValue().first;
            String code = itr.getValue().second;

            String className = cgp.getSDNodeInfo(node).getSDClassName();
            String var = className.equals("SDNode") ? "n" : "inN";
            os.printf("private SDValue transform_%s(SDNode %s){%n", itr.getKey(), var);
            if (!className.equals("SDNode"))
            {
                os.printf("%s assert %s instanceof %s;%n", ident, var, className);
                os.printf("%s%s n = (%s)%s;%n", ident, className, className, var);
            }
            code = stripOutNewLineInEnding(code);
            if (code.isEmpty()) code = "  return null;";
            os.printf("%s%n}%n", code);
        }
    }

    private void emitPredicates(PrintStream os) throws Exception
    {
        String ident = Util.fixedLengthString(2, ' ');
        TreeMap<String, Pair<Record, TreePattern>> predsByName = new TreeMap<>();

        for (Map.Entry<Record, TreePattern> itr : cgp.getPatternFragments().entrySet())
        {
            predsByName.put(itr.getKey().getName(), Pair.get(itr.getKey(), itr.getValue()));
        }

        os.printf("%s%s// Node Predicates Function.%n", ident, ident);
        for (Map.Entry<String, Pair<Record, TreePattern>> itr : predsByName.entrySet())
        {
            Record patFrag = itr.getValue().first;
            TreePattern pat = itr.getValue().second;

            String code = patFrag.getValueAsCode("Predicate");
            if (code == null || code.isEmpty()) continue;

            if (pat.getOnlyTree().isLeaf())
            {
                os.printf("private boolean predicate_%s(SDNode n) {%n", patFrag.getName());
            }
            else
            {
                String className = cgp.getSDNodeInfo(pat.getOnlyTree().getOperator()).getSDClassName();
                String var = className.equals("SDNode") ?"n":"inN";
                os.printf("private boolean predicate_%s(SDNode %s) {%n", patFrag.getName(), var);
                if (!className.equals("SDNode"))
                {
                    os.printf("%s assert %s instanceof %s;%n", ident, var, className);
                    os.printf("%s%s n = (%s)%s;%n", ident, className, className, var);
                }
            }
            code = stripOutNewLineInEnding(code);
            if (code.charAt(code.length()-1)=='\n')
                os.printf("%s}%n", code);
            else
                os.printf("%s%n}%n", code);
        }
    }
    /**
     * Considering the specified pattern to match, emit code to
     * the specified stream to match the pattern, and generate the
     * code for the match if it succedds. Returns true if the pattern is
     * not guaranted to match.
     */
    private void generateCodeForPattern(PatternToMatch pattern,
            ArrayList<Pair<GeneratedCodeKind, String>> generatedCode,
            TreeSet<String> generatedDecls, ArrayList<String> targetOpcodes,
            ArrayList<String> targetVTs,
            OutParamWrapper<Boolean> outputIsVariadic,
            OutParamWrapper<Integer> numInputRootOps) throws Exception
    {
        // create an instance served as emitting pattern code.
        PatternCodeEmitter emitter = new PatternCodeEmitter(
                cgp, pattern.getPredicateCheck(),
                pattern.getSrcPattern(),
                pattern.getDstPattern(),
                generatedCode, generatedDecls,
                targetOpcodes, targetVTs);

        emitter.emitMatchCode(pattern.getSrcPattern(), null, "n", "", false);

        TreePattern tp = cgp.getPatternFragments().firstEntry().getValue();

        TreePatternNode pat = pattern.getSrcPattern().clone();
        removeAllTypes(pat);

        do
        {
            try
            {
                boolean madeChange = true;
                while (madeChange)
                {
                    madeChange = pat.applyTypeConstraints(tp, true);
                }
            }
            catch(Exception e)
            {
                assert false:"Error: can't find consistent types for something we already decided was ok!";
                System.exit(-1);
            }

        }while(emitter.insertOneTypeCheck(pat, pattern.getSrcPattern(), "n", true));

        emitter.emitResultCode(pattern.getDstPattern(), pattern.getDstRegs(),
                false, false, false, false);
        outputIsVariadic.set(emitter.isOutputIsVariadic());
        numInputRootOps.set(emitter.getNumInputRootOps());
    }

    /**
     * Erase one code line from all of patterns. If removes
     * a line causes any of them to be empty, remove them
     * and return true when done.
     */
    private static boolean eraseCodeLine(
            LinkedList<Pair<PatternToMatch, LinkedList<Pair<GeneratedCodeKind, String>>>> patterns)
    {
        boolean erasedPatterns = false;
        for (int i = 0, e = patterns.size(); i < e; i++)
        {
            int sz = patterns.get(i).second.size();
            patterns.get(i).second.remove(sz-1);
            if (patterns.get(i).second.isEmpty())
            {
                patterns.remove(i);
                --i;
                --e;
                erasedPatterns = true;
            }
        }
        return erasedPatterns;
    }

    /**
     * Emit code for at least one pattern, but attempt to
     * group common code together between pattern to reduce
     * generated code size.
     */
    private void emitPatterns(
            LinkedList<Pair<PatternToMatch, LinkedList<Pair<GeneratedCodeKind, String>>>> patterns,
            int indent,
            PrintStream os) throws Exception
    {
        if (patterns.isEmpty()) return;

        int sz = patterns.get(patterns.size() - 1).second.size();
        Pair<GeneratedCodeKind, String> firstCodeLine =
                patterns.getLast().second.getLast();
        int lastMatch = patterns.size() - 1;
        while (lastMatch != 0 && patterns.getLast().second.getLast().equals(firstCodeLine))
        {
            --lastMatch;
        }

        if (lastMatch != 0)
        {
            LinkedList<Pair<PatternToMatch, LinkedList<Pair<GeneratedCodeKind, String>>>>
                    shared = new LinkedList<>(),
                    other = new LinkedList<>();
            shared.addAll(patterns.subList(lastMatch, patterns.size()));
            other.addAll(patterns.subList(0, lastMatch));

            if (shared.size() == 1)
            {
                PatternToMatch pat = shared.getLast().first;
                os.printf("%n%s// Pattern: ", Util.fixedLengthString(indent, ' '));
                pat.getSrcPattern().print(os);

                os.printf("%n%s// Emits: ", Util.fixedLengthString(indent, ' '));
                pat.getDstPattern().print(os);

                os.println();

                int addComplexity = pat.getAddedComplexity();
                os.printf("%s// Pattern complexity = %d, cost = %d, size = %d%n",
                        Util.fixedLengthString(indent, ' '),
                        getPatternSize(pat.getSrcPattern(), cgp) + addComplexity,
                        getResultPatternCost(pat.getDstPattern(), cgp),
                        getResultPatternSize(pat.getDstPattern(), cgp));
            }
            if (firstCodeLine.first != ExitPredicate)
            {
                os.printf("%s{%n", Util.fixedLengthString(indent, ' '));
                indent += 2;
            }
            emitPatterns(shared, indent, os);
            if (firstCodeLine.first != ExitPredicate)
            {
                indent += 2;
                os.printf("%s{%n", Util.fixedLengthString(indent, ' '));
            }

            if (other.size() == 1)
            {
                PatternToMatch pat = other.get(shared.size()-1).first;
                os.printf("%n%s// Pattern: ", Util.fixedLengthString(indent, ' '));
                pat.getSrcPattern().print(os);

                os.printf("%n%s// Emits: ", Util.fixedLengthString(indent, ' '));
                pat.getDstPattern().print(os);

                os.println();

                int addComplexity = pat.getAddedComplexity();
                os.printf("%s// Pattern complexity = %d, cost = %d, size = %d%n",
                        Util.fixedLengthString(indent, ' '),
                        getPatternSize(pat.getSrcPattern(), cgp) + addComplexity,
                        getResultPatternCost(pat.getDstPattern(), cgp),
                        getResultPatternSize(pat.getDstPattern(), cgp));
            }
            emitPatterns(other, indent, os);
            return;
        }

        boolean erasedPatterns = eraseCodeLine(patterns);
        boolean isPredicate = firstCodeLine.first == ExitPredicate;

        if (!isPredicate)
        {
            os.printf("%s%s%n", Util.fixedLengthString(indent, ' '), firstCodeLine.second);
        }
        else
        {
            os.printf("%s if (%s", Util.fixedLengthString(indent, ' '), firstCodeLine.second);

            while (!erasedPatterns && patterns.getLast().second.getLast().first == ExitPredicate)
            {
                boolean allEndWithSamePredicate = true;
                int e = patterns.size();
                Pair<GeneratedCodeKind, String> lastItem = patterns.getLast().second.getLast();
                for (int i = 0; i < e; i++)
                {
                    if (!patterns.get(i).second.get(patterns.get(i).second.size()-1).equals(lastItem))
                    {
                        allEndWithSamePredicate = false;
                        break;
                    }
                }
                if (!allEndWithSamePredicate) break;

                os.printf(" && %n%s%s", Util.fixedLengthString(indent+4, ' '),
                        lastItem.second);
                erasedPatterns = eraseCodeLine(patterns);
            }

            os.println(") {");
            indent += 2;
        }

        emitPatterns(patterns, indent, os);
        if (isPredicate)
            os.printf("%s}%n", Util.fixedLengthString(indent+4, ' '));
    }

    private static String getLegalJavaName(String opName)
    {
        int idx = opName.indexOf(".");
        if (idx != -1)
        {
            opName = opName.substring(0, idx) + opName.substring(idx+2, opName.length());
        }
        return opName;
    }

    private void emitInstructionSelector(PrintStream os) throws Exception
    {
        CodeGenTarget target = cgp.getTarget();

        TreeMap<String, ArrayList<PatternToMatch>> patternsByOpcode =
                new TreeMap<>();
        TreeMap<String, Integer> emitFunctions = new TreeMap<>();
        for (PatternToMatch pattern : cgp.getPatternsToMatch())
        {
            TreePatternNode node = pattern.getSrcPattern();
            if (!node.isLeaf())
            {
                String opName = getOpcodeName(node.getOperator(), cgp);
                if (!patternsByOpcode.containsKey(opName))
                    patternsByOpcode.put(opName, new ArrayList<>());
                patternsByOpcode.get(opName).add(pattern);
            }
            else
            {
                ComplexPattern cp;
                if (node.getLeafValue() instanceof IntInit)
                {
                    IntInit ii = (IntInit)node.getLeafValue();
                    String opName = getOpcodeName(cgp.getSDNodeNamed("imm"),cgp);
                    if (!patternsByOpcode.containsKey(opName))
                        patternsByOpcode.put(opName, new ArrayList<>());
                    patternsByOpcode.get(opName).add(pattern);
                }
                else if ((cp = nodeGetComplexPattern(node, cgp)) != null)
                {
                    ArrayList<Record> opNodes = cp.getRootNodes();
                    for (Record rec : opNodes)
                    {
                        String opName = getOpcodeName(rec, cgp);
                        if (!patternsByOpcode.containsKey(opName))
                            patternsByOpcode.put(opName, new ArrayList<>());
                        patternsByOpcode.get(opName).add(0, pattern);
                    }
                }
                else
                {
                    System.err.print("Unrecognized opcode '");
                    node.dump();
                    System.err.print("' on pattern '");
                    System.err.println(pattern.getDstPattern().getOperator().getName()+"'!");
                    System.exit(-1);
                }
            }
        }

        TreeMap<String, ArrayList<String>> opcodeVTMap = new TreeMap<>();

        for (Map.Entry<String, ArrayList<PatternToMatch>> itr : patternsByOpcode.entrySet())
        {
            String opName = itr.getKey();
            ArrayList<PatternToMatch> patternOfOps = itr.getValue();
            assert !patternOfOps.isEmpty():"No patterns but map has entry?";

            // split the patterns into groups by type.
            TreeMap<Integer, ArrayList<PatternToMatch>> patternsByType = new TreeMap<>();
            for (PatternToMatch op : patternOfOps)
            {
                TreePatternNode srcPat = op.getSrcPattern();
                int ty = srcPat.getTypeNum(0);
                if (!patternsByType.containsKey(ty))
                    patternsByType.put(ty, new ArrayList<>());
                patternsByType.get(ty).add(op);
            }

            for (Map.Entry<Integer, ArrayList<PatternToMatch>> itr2 : patternsByType.entrySet())
            {
                int opVT = itr2.getKey();
                ArrayList<PatternToMatch> patterns = itr2.getValue();

                ArrayList<Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>>> codeForPatterns = new ArrayList<>();
                ArrayList<ArrayList<String>> patternOpcodes = new ArrayList<>();
                ArrayList<ArrayList<String>> patternVTs = new ArrayList<>();
                ArrayList<TreeSet<String>> patternDecls = new ArrayList<>();
                ArrayList<Boolean> outputIsVariadicFlags = new ArrayList<>();
                ArrayList<Integer> numInputRootOpsCounts = new ArrayList<>();
                for (PatternToMatch pat : patterns)
                {
                    ArrayList<Pair<GeneratedCodeKind, String>> generatedCode = new ArrayList<>();
                    TreeSet<String> generatedDecls = new TreeSet<>();
                    ArrayList<String> targetOpcodes = new ArrayList<>();
                    ArrayList<String> targetVTs = new ArrayList<>();
                    OutParamWrapper<Boolean> outputIsVariadic = new OutParamWrapper<>(false);
                    OutParamWrapper<Integer> numInputRootOps = new OutParamWrapper<>(0);
                    generateCodeForPattern(pat, generatedCode, generatedDecls,
                            targetOpcodes, targetVTs, outputIsVariadic, numInputRootOps);

                    codeForPatterns.add(Pair.get(pat, generatedCode));
                    patternDecls.add(generatedDecls);
                    patternOpcodes.add(targetOpcodes);
                    patternVTs.add(targetVTs);
                    outputIsVariadicFlags.add(outputIsVariadic.get());
                    numInputRootOpsCounts.add(numInputRootOps.get());
                }

                for (int i = 0, e = codeForPatterns.size(); i < e; i++)
                {
                    ArrayList<Pair<GeneratedCodeKind, String>> generatedCode = codeForPatterns.get(i).second;
                    ArrayList<String> targetOpcodes = patternOpcodes.get(i);
                    ArrayList<String> targetVTs = patternVTs.get(i);
                    TreeSet<String> decls = patternDecls.get(i);
                    boolean outputIsVariadic = outputIsVariadicFlags.get(i);
                    int numInputRootOps = numInputRootOpsCounts.get(i);
                    ArrayList<String> addedInits = new ArrayList<>();
                    int cdoeSize = generatedCode.size();
                    int lastPred = -1;
                    for (int j = cdoeSize-1; j >= 0; j--)
                    {
                        if (lastPred == -1 && generatedCode.get(j).first == ExitPredicate)
                        {
                            lastPred = j;
                        }
                        else if (lastPred != -1 && generatedCode.get(j).first == Init)
                            addedInits.add(generatedCode.get(j).second);
                    }

                    StringBuilder calleeCode = new StringBuilder("(SDValue n");
                    StringBuilder callerCode = new StringBuilder("(n");
                    for (int j = 0, sz = targetOpcodes.size(); j < sz; j++)
                    {
                        calleeCode.append(", int opc").append(j);
                        callerCode.append(", ").append(targetOpcodes.get(j));
                    }

                    for (int j = 0, sz = targetVTs.size(); j < sz; j++)
                    {
                        calleeCode.append(", EVT vt").append(j);
                        callerCode.append(", ").append(targetVTs.get(j));
                    }

                    for (String decl : decls)
                    {
                        calleeCode.append(", SDValue ").append(decl);
                        callerCode.append(", ").append(decl);
                    }

                    if (outputIsVariadic)
                    {
                        calleeCode.append(", int numInputRootOps");
                        callerCode.append(", ").append(numInputRootOps);
                    }

                    calleeCode.append(")");
                    callerCode.append(");");
                    calleeCode.append("{\n");

                    for (String val : addedInits)
                        calleeCode.append(" ").append(val).append("\n");

                    for (int j = lastPred+1; j < cdoeSize; j++)
                        calleeCode.append(" ").append(generatedCode.get(j).second).append("\n");

                    for (int j = lastPred+1; j < cdoeSize; j++)
                        generatedCode.remove(generatedCode.size()-1);

                    calleeCode.append("}\n");

                    int emitFuncNum;
                    if (emitFunctions.containsKey(calleeCode.toString()))
                        emitFuncNum = emitFunctions.get(calleeCode.toString());
                    else
                    {
                        emitFuncNum = emitFunctions.size();
                        emitFunctions.put(calleeCode.toString(), emitFuncNum);
                        os.printf("SDNode emit_%d%s", emitFuncNum, calleeCode);
                    }

                    callerCode.insert(0, "SDNode result = emit_"+emitFuncNum);
                    generatedCode.add(Pair.get(Init, callerCode.toString()));
                    generatedCode.add(Pair.get(Normal, "return result;"));
                }

                // print function.
                String opVTStr = "";
                if (opVT == MVT.iPTR)
                    opVTStr = "_iPTR";
                else if (opVT == MVT.iPTRAny)
                    opVTStr = "_iPTRAny";
                else if (opVT == MVT.isVoid)
                {
                    // nothing to do.
                }
                else
                {
                    opVTStr = "_" + getEnumName(opVT).substring(0, 5);
                }

                if (!opcodeVTMap.containsKey(opName))
                {
                    ArrayList<String> vtSet = new ArrayList<>();
                    vtSet.add(opVTStr);
                    opcodeVTMap.put(opName, vtSet);
                }
                else
                    opcodeVTMap.get(opName).add(opVTStr);

                codeForPatterns.sort(new PatternSortingPredicate(cgp));
                boolean mightNotMatch = true;
                /*
                for (int i = 0, e = codeForPatterns.size(); i < e; i++)
                {
                    ArrayList<Pair<GeneratedCodeKind, String>> generatedCode = codeForPatterns.get(i).second;
                    mightNotMatch = false;

                    for (int j = 0, sz = generatedCode.size(); j < sz; j++)
                    {
                        if (generatedCode.get(j).first == ExitPredicate)
                        {
                            mightNotMatch = true;
                            break;
                        }
                    }
                }
                if (!mightNotMatch)// && i != codeForPatterns.size() - 1)
                {
                    System.err.println("Pattern '");
                    //codeForPatterns.get(i).first.getSrcPattern().print(System.err);
                    System.err.println("' is impossible to select!");
                    System.exit(-1);
                }*/

                for (int i = 0, e = codeForPatterns.size(); i < e; i++)
                {
                    ArrayList<Pair<GeneratedCodeKind, String>> generatedCode = codeForPatterns.get(i).second;
                    Collections.reverse(generatedCode);
                }

                Collections.reverse(codeForPatterns);

                os.printf("SDNode select_%s%s(SDValue n) {%n",
                        getLegalJavaName(opName),
                        opVTStr);

                LinkedList<Pair<PatternToMatch, LinkedList<Pair<GeneratedCodeKind, String>>>> temp = new LinkedList<>();
                for (Pair<PatternToMatch, ArrayList<Pair<GeneratedCodeKind, String>>> itr3 : codeForPatterns)
                {
                    LinkedList<Pair<GeneratedCodeKind, String>> list = new LinkedList<>();
                    list.addAll(itr3.second);
                    temp.add(Pair.get(itr3.first,list));
                }
                emitPatterns(temp, 2, os);

                if (mightNotMatch)
                {
                    os.println();
                    if (!Objects.equals(opName, "ISD.INTRINSIC_W_CHAIN") &&
                            !Objects.equals(opName, "ISD.INTRINSIC_WO_CHAIN") &&
                            !Objects.equals(opName, "ISD.INTRINSIC_VOID"))
                    {
                        os.println("  cannotYetSelect(n);");
                    }
                    else
                        os.println("  cannotYetSelectIntrinsic(n);");
                    os.println("  return null;");
                }
                os.println("}\n");
            }
        }

        // Emit boilerplate.
        os.print("SDNode select_INLINEASM(SDValue n) {\n"
                        + "  ArrayList<SDValue> ops = new ArrayList<>();"
                        + "  for (int i = 0, e = n.getNumOperands(); i < e; i++) ops.add(n.getOperand(i));\n"
                        + "  selectInlineAsmMemoryOperands(ops);\n\n"

                        + "  ArrayList<EVT> vts = new ArrayList<>();\n"
                        + "  vts.add(new EVT(MVT.Other));\n"
                        + "  vts.add(new EVT(MVT.Flag));\n"
                        + "  SDValue newNode = curDAG.getNode(ISD.INLINEASM,"
                        + "  vts);\n"
                        + "  return newNode.getNode();\n"
                        + "}\n\n");

        os.print("SDNode select_UNDEF(SDValue n) {\n"
                + "  return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.IMPLICIT_DEF,\n"
                + "                              n.getValueType());\n"
                + "}\n\n");

        os.print("SDNode Select_DBG_LABEL(SDValue n) {\n"
                + "  SDValue chain = n.getOperand(0);\n"
                + "  int c = ((LabelSDNode)n).getLabelID();\n"
                + "  SDValue tmp = curDAG.getTargetConstant(c, new EVT(MVT.i32));\n"
                + "  return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.DBG_LABEL,\n"
                + "                              new EVT(MVT.Other), tmp, chain);\n"
                + "}\n\n");

        os.print("SDNode select_EH_LABEL(SDValue n) {\n"
                + "  SDValue chain = n.getOperand(0);\n"
                + "  int c = ((LabelSDNode)n).getLabelID();\n"
                + "  SDValue tmp = curDAG.getTargetConstant(c, new EVT(MVT.i32));\n"
                + "  return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.EH_LABEL,\n"
                + "                              new EVT(MVT.Other), tmp, chain);\n"
                + "}\n\n");

        os.print("SDNode select_DECLARE(SDValue n) {\n"
                + "  SDValue chain = n.getOperand(0);\n"
                + "  SDValue n1 = n.getOperand(1);\n"
                + "  SDValue n2 = n.getOperand(2);\n"
                + "  if (!(n1 instanceof FrameIndexSDNode) || !(n2 instanceof GlobalAddressSDNode)) {\n"
                + "    cannotYetSelect(n);\n"
                + "  }\n"
                + "  int fi = ((FrameIndexSDNode)n1).getIndex();\n"
                + "  GlobalValue gv = ((GlobalAddressSDNode)n2).getGlobal();\n"
                + "  SDValue tmp1 = "
                + "curDAG.getTargetFrameIndex(fi, tli.getPointerTy());\n"
                + "  SDValue tmp2 = "
                + "curDAG.getTargetGlobalAddress(gv, tli.getPointerTy());\n"
                + "  return curDAG.selectNodeTo(n.getNode(), TargetInstrInfo.DECLARE,\n"
                + "                              new EVT(MVT.Other), tmp1, tmp2, chain);\n"
                + "}\n\n");

        os.print("// The main instruction selector code.\n"
                + "SDNode selectCode(SDValue n) {\n"
                + "  int nvt = n.getNode().getValueType(0).getSimpleVT().SimpleTy;\n"
                + "  switch (n.getOpcode()) {\n"
                + "  default:\n"
                + "    assert !n.isMachineOpcode() : \"Node already selected!\";\n"
                + "    break;\n"
                + "  case ISD.EntryToken:       // These nodes remain the same.\n"
                + "  case ISD.MEMOPERAND:\n"
                + "  case ISD.BasicBlock:\n"
                + "  case ISD.Register:\n"
                + "  case ISD.HANDLENODE:\n"
                + "  case ISD.TargetConstant:\n"
                + "  case ISD.TargetConstantFP:\n"
                + "  case ISD.TargetConstantPool:\n"
                + "  case ISD.TargetFrameIndex:\n"
                + "  case ISD.TargetExternalSymbol:\n"
                + "  case ISD.TargetJumpTable:\n"
                + "  case ISD.TargetGlobalTLSAddress:\n"
                + "  case ISD.TargetGlobalAddress:\n"
                + "  case ISD.TokenFactor:\n"
                + "  case ISD.CopyFromReg:\n"
                + "  case ISD.CopyToReg: {\n"
                + "    return null;\n"
                + "  }\n"
                + "  case ISD.AssertSext:\n"
                + "  case ISD.AssertZext: {\n"
                + "    replaceUses(n, n.getOperand(0));\n"
                + "    return null;\n"
                + "  }\n"
                + "  case ISD.INLINEASM: return select_INLINEASM(n);\n"
                + "  case ISD.DBG_LABEL: return select_DBG_LABEL(n);\n"
                + "  case ISD.EH_LABEL: return select_EH_LABEL(n);\n"
                + "  case ISD.DECLARE: return select_DECLARE(n);\n"
                + "  case ISD.UNDEF: return select_UNDEF(n);\n");

        for (Map.Entry<String, ArrayList<PatternToMatch>> itr : patternsByOpcode.entrySet())
        {
            String opName = itr.getKey();

            ArrayList<String> opVTs = opcodeVTMap.get(opName);
            os.println("  case " + opName + ": {");
            if (opVTs.size() == 1 && opVTs.get(0).isEmpty())
            {
                os.println("   return select_" + getLegalJavaName(opName) + "(n);");
                os.println("    break;");
                os.println("  }");
                continue;
            }

            boolean hasPtrPattern = false;
            boolean hasDefaultPattern = false;

            os.println("    switch(nvt) {");
            for (String vtStr : opVTs)
            {
                if (vtStr.isEmpty())
                {
                    hasDefaultPattern = true;
                    continue;
                }

                if (vtStr.equals("_iPTR"))
                {
                    hasPtrPattern = true;
                    continue;
                }
                os.println("    case MVT." + vtStr.substring(0, 1)
                        + ":\n" +
                        "    return select_" + getLegalJavaName(opName)
                        + vtStr + "(n);");
            }
            os.println("    default:");

            if (hasPtrPattern)
            {
                os.println("    if (tli.getPointerTy() == nvt)");
                os.println("    return select_" + getLegalJavaName(opName) + "_iPTR(n);");
            }
            if (hasDefaultPattern)
            {
                os.println("    return select_"+getLegalJavaName(opName)+"(n);");
            }
            os.println("    break;");
            os.println("  }");
            os.println("    break;");
            os.println("  }");

        }

        os.print("  } // end of big switch.\n\n"
                + "  if (n.getOpcode() != ISD.INTRINSIC_W_CHAIN &&\n"
                + "      n.getOpcode() != ISD.INTRINSIC_WO_CHAIN &&\n"
                + "      n.getOpcode() != ISD.INTRINSIC_VOID) {\n"
                + "    cannotYetSelect(n);\n"
                + "  } else {\n"
                + "    cannotYetSelectIntrinsic(n);\n"
                + "  }\n"
                + "  return null;\n"
                + "}\n\n");

        os.print("void cannotYetSelect(SDValue N) {\n"
                + "  String msg;\n"
                + "  raw_string_ostream Msg(msg);\n"
                + "  Msg + \"Cannot yet select: \";\n"
                + "  n.getNode().print(msg, curDAG);\n"
                + "  llvm_report_error(msg.toString());\n"
                + "}\n\n");

        os.print("void cannotYetSelectIntrinsic(SDValue n) {\n"
                + "  System.err.println(\"Cannot yet select: \");\n"
                + "  int iid = ((ConstantSDNode)n.getOperand("
                + "n.getOperand(0).getValueType() == MVT.Other?1:0)).getZExtValue();\n"
                + " llvm_report_error(\"Cannot yet select: intrinsic %\" +\n"
                + "intrinsic.getName(iid));\n"
                + "}\n\n");
    }

    @Override
    public void run(String outputFile) throws Exception
    {
        assert outputFile != null && !outputFile.isEmpty();
        try(PrintStream os = !outputFile.equals("-") ?
                new PrintStream(new FileOutputStream(outputFile)) :
                System.out)
        {
            CodeGenTarget target = cgp.getTarget();
            String targetName = target.getName();
            os.printf("package backend.target.%s;%n%n", targetName.toLowerCase());
            os.println("import backend.codegen.MVT;");
            os.println("import backend.codegen.dagisel.*;");
            os.println("import backend.codegen.dagisel.SDNode.*;");
            os.println("import backend.codegen.fastISel.ISD;");
            os.println("import backend.target.TargetInstrInfo;");
            os.println("import backend.target.TargetMachine;");
            os.println("import backend.type.PointerType;");
            os.println("import backend.value.Value;");

            emitHeader(os, targetName);

            emitNodeTransform(os);
            emitPredicates(os);

            emitInstructionSelector(os);
            os.println("}");
        }
    }
}
