package utils.tablegen;
/*
 * Xlous C language Compiler
 * Copyright (c) 2015-2017, Xlous
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
 * This file defines a abstract class that provides some desired common interface
 * methods for various table gen backend, e.g. {@linkplain RegisterInfoEmitter},
 * {@linkplain InstrInfoEmitter}.
 * @author Xlous.zeng
 * @version 0.1
 */
public abstract class TableGenBackend
{
    /**
     * All tablegen backends should implement the run method, which should be the main
     * entry point.
     */
    public abstract void run(String outputFile) throws Exception;

    /**
     * Output a Java style header comment on the generated Java source file.
     * @param desc
     * @param os
     */
    public void emitSourceFileHeaderComment(String desc, PrintStream os)
    {
        os.println("/**");
        os.printf(" * %s","TableGen created file.");
        os.println();
        os.println(" * <p>");
        os.printf(" * %s\n", desc);
        os.println(" * Automatically generated by .td file, do not edit!");
        os.println(" * Powered by Xlous zeng");
        os.println(" * </p>");
        os.println(" */");
    }
}