package backend.support;
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

import jlang.diag.Diagnostic;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class ErrorHandling
{
    public interface LLVMErrorHandler
    {
        void apply(Diagnostic diag, String msg);
    }

    private static LLVMErrorHandler errorHandler;
    private static Diagnostic diagEngineer;

    public static void llvmReportError(String msg)
    {
        if (errorHandler != null)
            errorHandler.apply(diagEngineer, msg);
        else
            System.err.printf(msg);
    }

    public static void installLLVMErrorHandler(LLVMErrorHandler handler, Diagnostic diag)
    {
        errorHandler = handler;
        diagEngineer = diag;
    }
}