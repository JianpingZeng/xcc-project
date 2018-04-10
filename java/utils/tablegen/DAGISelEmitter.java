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

import java.io.FileOutputStream;
import java.io.PrintStream;

public class DAGISelEmitter extends TableGenBackend
{
    private RecordKeeper records;
    private CodeGenDAGPatterns cgp;
    private String className;

    public DAGISelEmitter(RecordKeeper rec) throws Exception
    {
        records = rec;
        cgp = new CodeGenDAGPatterns(records);
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
            os.println("import backend.codegen.dagisel.SDNode;");
            os.println("import backend.codegen.dagisel.SDNode;");
            os.println("import backend.codegen.dagisel.SDValue;");
            os.println("import backend.target.TargetMachine;");

            emitSourceFileHeaderComment("Instruction Selection based on DAG Covering.", os);

            os.printf("public final class %sGenDAGToDAGISel extends %sDAGToDAGISel%n{%n", targetName, targetName);

            os.println("public X86GenDAGToDAGISel(X86TargetMachine tm, TargetMachine.CodeGenOpt optLevel)\n" +
                    "\t{\n" +
                    "\t\tsuper(tm, optLevel);\n" +
                    "\t}");

            os.println("\tprotected SDNode selectCode(SDValue node) { return null; }");

            os.println("}");
        }
    }
}
