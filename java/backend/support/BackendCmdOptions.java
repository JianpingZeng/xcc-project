package backend.support;
/*
 * Xlous C language Compiler
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

import backend.codegen.PrologEpilogInserter;
import backend.passManaging.PMDataManager;
import tools.commandline.*;

import static backend.codegen.PrologEpilogInserter.ShrinkWrapDebugLevel;
import static backend.passManaging.PMDataManager.PassDebugLevel;
import static tools.commandline.Desc.desc;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionNameApplicator.optionName;

/**
 * This is a utility class for registering Backend command line options into
 * CommandLine DataBase.
 * @author Xlous.zeng
 * @version 0.1
 */
public class BackendCmdOptions
{
    public static final Opt<PMDataManager.PassDebugLevel> PassDebugging =
            new Opt<PMDataManager.PassDebugLevel>(
                    new Parser<>(),
                    new OptionNameApplicator("debug-pass"),
                    new OptionHiddenApplicator(Hidden),
                    Desc.desc("Print PassManager debugging information"),
                    Initializer.init(PassDebugLevel.None),
                    new ValueClass<>(
                            new ValueClass.Entry<>(PassDebugLevel.None, "none", "disable debug output"),
                            new ValueClass.Entry<>(PassDebugLevel.Arguments, "arguments", "print pass arguments to pass to 'opt'"),
                            new ValueClass.Entry<>(PassDebugLevel.Structures, "structures", "print pass structure before run()"),
                            new ValueClass.Entry<>(PassDebugLevel.Executions, "executions", "print pass name before it is executed"),
                            new ValueClass.Entry<>(PassDebugLevel.Details, "details", "print pass details when it is executed")
                    )
            );
    public static final Opt<PrologEpilogInserter.ShrinkWrapDebugLevel> ShrinkWrapDebugging =
            new Opt<PrologEpilogInserter.ShrinkWrapDebugLevel>(
            new Parser<>(), optionName("shrink-wrap-dbg"),
            new OptionHiddenApplicator(Hidden),
            desc("Print shrink wrapping debugging information"),
            new ValueClass<>(
                    new ValueClass.Entry<>(
                            PrologEpilogInserter.ShrinkWrapDebugLevel.None, "None",
                            "disable debug output"),
                    new ValueClass.Entry<>(ShrinkWrapDebugLevel.BasicInfo, "BasicInfo",
                            "print basic DF sets"),
                    new ValueClass.Entry<>(
                            PrologEpilogInserter.ShrinkWrapDebugLevel.Iterations,
                            "Iterations", "print SR sets for each iteration"),
                    new ValueClass.Entry<>(ShrinkWrapDebugLevel.Details,
                            "Details", "print all DF sets")));

    /**
     * A method used for registering Backend command line options.
     * </br>
     * This method must be called before calling to
     * {@linkplain tools.commandline.CL#parseCommandLineOptions(String[])}.
     */
    public static void registerBackendCommandLineOptions()
    {
    }
}
