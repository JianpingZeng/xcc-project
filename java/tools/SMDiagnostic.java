package tools;
/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2017, Xlous Zeng.
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

import java.io.PrintStream;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public final class SMDiagnostic
{
    private String filename;
    private int lineNo, columnNo;
    private String message, lineContents;

    public SMDiagnostic()
    {
        lineNo = 0;
        columnNo = 0;
    }

    public SMDiagnostic(String filename,
            int lineNo, int columnNo,
            String msg, String lineContents)
    {
        this.filename = filename;
        this.lineNo = lineNo;
        this.columnNo = columnNo;
        this.message = msg;
        this.lineContents = lineContents;
    }

    public void print(String progName, PrintStream os)
    {
        if (progName != null && !progName.isEmpty())
            os.printf("%s: ", progName);

        if (filename.equals("-"))
            os.printf("<stdin>");
        else
            os.print(filename);

        if (lineNo != -1)
        {
            os.printf(":%d", lineNo);
            if (columnNo != -1)
                os.printf(":%d", columnNo+1);
        }

        os.printf(": %s%n", message);
        if (lineNo != -1 && columnNo != -1)
        {
            os.println(lineContents);

            /// print out spaces/tabs before caret.
            for (int i = 0; i < columnNo; i++)
                os.print(lineContents.charAt(i) == '\t'?'\t':' ');

            os.println("^");
        }
    }
}
