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

import tools.Pair;
import tools.Util;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

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
        //os.println("\tprotected SDNode selectCode(SDValue node) { return null; }");
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

    private void emitSelector(PrintStream os)
    {
        os.println("@Override");
        os.println("public SDNode selectCode(SDValue node) { return null; }");
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

            emitSelector(os);
            os.println("}");
        }
    }
}
