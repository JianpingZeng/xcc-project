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

import backend.codegen.*;
import backend.passManaging.PMDataManager;
import tools.commandline.*;

import static backend.codegen.AsmPrinter.BoolOrDefault.BOU_UNSET;
import static backend.codegen.PrologEpilogInserter.ShrinkWrapDebugLevel;
import static backend.passManaging.PMDataManager.PassDebugLevel;
import static tools.commandline.Desc.desc;
import static tools.commandline.Initializer.init;
import static tools.commandline.OptionHidden.Hidden;
import static tools.commandline.OptionNameApplicator.optionName;
import static tools.commandline.ValueDesc.valueDesc;

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
    // Shrink Wrapping:
    public static final BooleanOpt ShrinkWrapping = new BooleanOpt(optionName("shrink-wrap"),
            init(false),
            desc("Shrink wrap callee-saved register spills/restores"));

    // Shrink wrap only the specified function, a debugging aid.
    public static final StringOpt ShrinkWrapFunc = new StringOpt(optionName("shrink-wrap-func"),
            desc("Shrink wrap the specified function"), valueDesc("funcname"),
            init(""));

    public static final Opt<AsmPrinter.BoolOrDefault> AsmVerbose = new Opt<AsmPrinter.BoolOrDefault>(
            new Parser<>(),
            optionName("asm-verbose"),
            desc("Add comments to directives."),
            init(BOU_UNSET)
    );
    public static final BooleanOpt NewAsmPrinter =
            new BooleanOpt(optionName("experimental-asm-printer"),
            new OptionHiddenApplicator(Hidden));

    public static final Opt<MachinePassCtor> RegAlloc =
            new Opt<MachinePassCtor>(new RegisterRegAllocParser(),
                    optionName("regalloc"),
                    desc("Register allocator to use: (default = local)"),
                    init((MachinePassCtor) RegAllocLocal::createLocalRegAllocator));

    public static final BooleanOpt UseDFSOnNumberMI =
            new BooleanOpt(optionName("use-dfs-numbering-mi"),
                    new OptionHiddenApplicator(Hidden),
                    desc("Use dfs order to number machine instr(default to true)"),
                    init(true));

    public static final BooleanOpt DisableRearrangementMBB =
            new BooleanOpt(optionName("disable-rearrangement-mbb"),
                    new OptionHiddenApplicator(Hidden),
                    desc("Disable rearrangement on machine function(default to true)"),
                    init(true));
    /**
     * Choose an appropriate register allocator according command line option.
     * @return
     */
    public static MachineFunctionPass createRegisterAllocator()
    {
        if (RegAlloc.value != null)
        {
            RegisterRegAlloc.setDefault(RegAlloc.value);
            return RegAlloc.value.apply();
        }

        MachinePassCtor ctor = RegisterRegAlloc.getDefault();
        if (ctor == null)
        {
            ctor = RegAlloc.value;
            RegisterRegAlloc.setDefault(ctor);
        }
        return ctor.apply();
    }

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
