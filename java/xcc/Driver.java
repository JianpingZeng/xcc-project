/*
 * Extremely C language Compiler
 * Copyright (c) 2015-2018, Xlous Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

package xcc;

import backend.support.Triple;
import config.Config;
import jlang.diag.CompilationPhase;
import jlang.diag.Diagnostic;
import jlang.diag.Diagnostic.DiagnosticBuilder;
import jlang.diag.Diagnostic.StaticDiagInfoRec;
import jlang.diag.FullSourceLoc;
import tools.Pair;
import tools.Util;
import xcc.Action.*;
import xcc.Option.InputOption;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static jlang.diag.CompilationPhase.*;
import static xcc.DiagnosticJlangDriverTag.*;
import static xcc.HostInfo.createLinuxHostInfo;
import static xcc.HostInfo.createUnknownHostInfo;
import static xcc.InputType.getNumCompilationPhases;
import static xcc.OptionID.*;
import static xcc.OptionKind.*;
import static xcc.TypeID.*;

/**
 * @author Xlous.zeng
 * @version 0.1
 */
public class Driver
{
    /**
     * An static interface used for registering a initializer for the purpose of
     * adding some diagnostic kinds into Diagnostic related with Jlang driver.
     */
    static
    {
        Diagnostic.DiagInitializer initializer = new Diagnostic.DiagInitializer()
        {
            @Override public StaticDiagInfoRec[] getDiagKinds()
            {
                DiagnosticJlangDriverKinds[] kinds = DiagnosticJlangDriverKinds.values();
                StaticDiagInfoRec[] recs = new StaticDiagInfoRec[kinds.length];
                int idx = 0;
                for (DiagnosticJlangDriverKinds kind : kinds)
                {
                    recs[idx++] = new StaticDiagInfoRec(kind.diagID,
                            kind.diagMapping, kind.diagClass, kind.sfinae, kind.text, kind.optionGroup);
                }
                return recs;
            }
        };

        Diagnostic.registerDiagInitialize(initializer);
    }

    private String defaultBasename;
    private String defaultDirname;
    private String defaultImagename;
    private Diagnostic theDiags;
    private String triple;
    private HostInfo host;
    private OptTable optTable;
    private boolean suppressMissingInputWarning;

    public Driver(String basename, String dirname, String hostTriple,
            String imageName, Diagnostic diags)
    {
        defaultBasename = basename;
        defaultDirname = dirname;
        defaultImagename = imageName;
        theDiags = diags;
        triple = hostTriple;
        optTable = new OptTable();
    }

    private DiagnosticBuilder diag(int diagID)
    {
        return theDiags.report(new FullSourceLoc(), diagID);
    }

    public OptTable getOptTable()
    {
        return optTable;
    }

    private InputArgList parseArgList(String[] args)
    {
        InputArgList argList = new InputArgList(args);
        for (int index = 0, sz = argList.getNumInputStrings(); index < sz; )
        {
            if (argList.getArgString(index).isEmpty())
            {
                ++index;
                continue;
            }

            int prev = argList.getIndex();
            Arg arg = getOptTable().parseOneArg(argList);
            int after = argList.getIndex();
            assert after >= prev;
            if (arg == null)
            {
                diag(err_drv_missing_argument)
                        .addString(argList.getArgString(prev))
                        .addTaggedVal(after - prev - 1).emit();
                continue;
            }
            if (arg.getOption().isUnsupported())
            {
                diag(err_drv_jlang_unsupported)
                        .addString(argList.getArgString(prev)).emit();
                continue;
            }
            argList.add(arg);
        }
        return argList;
    }

    private HostInfo getHostInfo(String tripleStr)
    {
        Triple defaultTriple = new Triple(tripleStr);
        if (defaultTriple.getArchName().equals("i686"))
            defaultTriple.setArchName("i386");
        else if (defaultTriple.getArchName().equals("amd64"))
            defaultTriple.setArchName("x86_64");
        else
            assert false : "Unknown architecture name!";
        switch (defaultTriple.getOS())
        {
            case Linux:
                return createLinuxHostInfo(this, defaultTriple);
            default:
                return createUnknownHostInfo(this, defaultTriple);
        }
    }

    public boolean useJlangAsCompiler(Compilation comp, JobAction ja,
            String archName)
    {
        return true;
    }

    /**
     * Constructs an Action for each Compilation phase as follow.
     */
    private Action constructAction(ArgList args, int phase, Action input)
    {
        switch (phase)
        {
            case Preprocess:
            {
                int outputTy;
                if (args.hasArg(OPT__M) || args.hasArg(OPT__MM))
                    outputTy = TY_Dependencies;
                else
                {
                    outputTy = InputType
                            .getPreprocessedType(input.getOutputType());
                    assert outputTy
                            != TY_INVALID : "can't preprocess this input type!";
                }
                return new PreprocessJobAction(input, outputTy);
            }
            case Precompile:
                return new PrecompileJobAction(input, TY_PCH);
            case Compile:
            {
                if (args.hasArg(OPT__fsyntax_only))
                {
                    return new CompileJobAction(input, TY_Nothing);
                }
                else if (args.hasArg(OPT__emit_llvm) || args.hasArg(OPT__flto)
                        || args.hasArg(OPT__O4))
                {
                    int outputTy = args.hasArg(OPT__S) ? TY_LLVMAsm : TY_LLVMBC;
                    return new CompileJobAction(input, outputTy);
                }
                else
                    return new CompileJobAction(input, TY_PP_Asm);
            }
            case Assemble:
            {
                return new AssembleJobAction(input, TY_Object);
            }
            default:
            {
                assert phase == Link;
                assert false : "Link should be handled in method buildActions!";
                return null;
            }
        }
    }

    private void buildActions(Compilation c)
    {
        ArgList args = c.getArgs();
        ToolChain tc = c.getToolChain();
        ArrayList<Action> linkerInputs = new ArrayList<>();

        int inputType = TY_Nothing;
        Arg inputTypeArg = null;

        ArrayList<Pair<Arg, Integer>> inputs = new ArrayList<>();

        for (int i = 0, e = args.size(); i < e; i++)
        {
            Arg arg = args.getArgs(i);
            Option opt = args.getOption(i);
            if (opt == null)
                continue;

            if (opt instanceof InputOption)
            {
                String value = arg.getValue(args, 0);
                int ty = TY_INVALID;

                if (inputType == TY_Nothing)
                {
                    if (inputTypeArg != null)
                        inputTypeArg.claim();

                    if (value.equals("-"))
                    {
                        if (!args.hasArg(OPT__E, false))
                            diag(err_drv_unknown_stdin_type).emit();
                        ty = TY_C;
                    }
                    else
                    {
                        int lastDot = value.lastIndexOf('.');
                        if (lastDot >= 0)
                            ty = InputType.lookupTypeForExtension(
                                    value.substring(lastDot + 1));
                        if (ty == TY_INVALID)
                            ty = TY_Object;
                    }

                    if (ty != TY_Object)
                    {
                        if (args.hasArg(OPT__ObjC))
                            ty = TY_ObjC;
                        else if (args.hasArg(OPT__ObjCXX))
                            ty = TY_ObjCXX;
                    }
                }
                else
                {
                    inputTypeArg.claim();
                    ty = inputType;
                }

                if (!value.equals("-") && !Files.exists(Paths.get(value)))
                {
                    diag(err_drv_no_such_file).addString(arg.getValue(args, 0)).emit();
                }
                else
                {
                    inputs.add(Pair.get(arg, ty));
                }
            }
            else if (opt.isLinkerInput())
            {
                inputs.add(Pair.get(arg, TY_Object));
            }
            else if (arg.getOption().getID() == OPT__x)
            {
                inputTypeArg = arg;
                inputType = InputType.lookupTypeForExtension(
                        arg.getValue(args, arg.getIndex() + 1));
            }
        }

        // Compute the final compilatio phase.
        int finalPhase;
        Arg finalPhaseArg = null;
        if ((finalPhaseArg = args.getLastArg(OPT__E, true)) != null
                || (finalPhaseArg = args.getLastArg(OPT__M, true)) != null
                || (finalPhaseArg = args.getLastArg(OPT__MM, true)) != null)
        {
            finalPhase = Preprocess;
        }
        else if ((finalPhaseArg = args.getLastArg(OPT__c, true)) != null)
        {
            finalPhase = Assemble;
        }
        else if ((finalPhaseArg = args.getLastArg(OPT__S, true)) != null
                || (finalPhaseArg = args.getLastArg(OPT__fsyntax_only, true)) != null)
        {
            finalPhase = Compile;
        }
        else
            // Other cases which we always treat as linker input.
            finalPhase = Link;

        for (Pair<Arg, Integer> entity : inputs)
        {
            int filetype = entity.second;
            Arg inputArg = entity.first;

            int numSteps = getNumCompilationPhases(filetype);
            assert numSteps > 0 : "Invalid number of steps!";

            int initialPhase = InputType.getCompilationPhase(filetype, 0);

            if (initialPhase > finalPhase)
            {
                inputArg.claim();
                diag(warn_drv_input_file_unused).addString(inputArg.getAsSTring(args))
                        .addString(getPhaseName(initialPhase))
                        .addString(finalPhaseArg.getOption().getName()).emit();
                continue;
            }

            Action current = new InputAction(inputArg, filetype);
            for (int i = 0; i < numSteps; i++)
            {
                int phase = InputType.getCompilationPhase(inputType, i);
                if (phase > finalPhase)
                    break;

                if (phase == Link)
                {
                    assert i + 1
                            == numSteps : "Linker must be final compilation step!";
                    linkerInputs.add(current);
                    current = null;
                    break;
                }
                if (phase == CompilationPhase.Assemble
                        && current.getOutputType() != TY_PP_Asm)
                    continue;

                current = constructAction(args, phase, current);
                assert current != null;
                if (current.getOutputType() == TY_Nothing)
                    break;

            }
            if (current != null)
                c.addAction(current);
        }
        if (!linkerInputs.isEmpty())
            c.addAction(new LinkJobAction(linkerInputs, TY_Image));
    }

    private String getPhaseName(int phase)
    {
        switch (phase)
        {
            case Preprocess:
                return "preprocess";
            case Precompile:
                return "precompile";
            case Compile:
                return "compile";
            case Assemble:
                return "assemble";
            case Link:
                return "link";
            default:
                assert false : "Invalid phase";
                return "";
        }
    }

    private void buildJobs(Compilation c)
    {
    }

    private static String getOptionHelpName(OptTable table, int id)
    {
        String name = table.getOptionName(id);

        switch (table.getOptionKind(id))
        {
            case KIND_Input:
            case KIND_Unknown:
                assert false:"Invalid option with help text";
            case KIND_MultiArg:
            case KIND_JoinedAndSeparate:
                assert false:"Can't print metavar for this kind of option";
            case KIND_Flag:
                break;
            case KIND_Separate:
            case KIND_JoinedOrSeparate:
                name += " ";
            case KIND_Joined:
            case KIND_CommaJoined:
                name += table.getOptionMetaVar(id);
                break;
        }
        return name;
    }

    private void printHelp(boolean printHidden)
    {
        System.out.println("OVERVIEW: jlang compiler driver");
        System.out.println();
        System.out.printf("USAGE: %s [options] <input files>%n%n",
                defaultBasename);
        System.out.println("OPTIONS:");

        ArrayList<Pair<String, String>> optionHelp = new ArrayList<>();
        int maximumOptionWidth = 0;
        for (int i = OPT__input_; i < OPT_LastOption; i++)
        {
            String text = getOptTable().getOptionHelpText(i);
            if (text != null)
            {
                String info = getOptionHelpName(getOptTable(), i);
                int length = info.length();
                if (length <= 23)
                    maximumOptionWidth = Math.max(length, maximumOptionWidth);

                optionHelp.add(Pair.get(info, text));
            }
        }
        for (int i = 0, e = optionHelp.size(); i < e; i++)
        {
            String option = optionHelp.get(i).first;
            System.out.printf("  %s%s", option, Util.fixedLengthString(maximumOptionWidth, ' '));
            System.out.printf(" %s%n", optionHelp.get(i).second);
        }
    }

    private void printVersion(Compilation c, PrintStream os)
    {
        os.printf("jlang version %s.%s", Config.XCC_Major, Config.XCC_Minor);
        ToolChain tc = c.getToolChain();
        os.printf("Target: %s%n", tc.getTripleString());
    }

    private boolean handleVersion(Compilation c)
    {
        if (c.getArgs().hasArg(OPT___help)
                || c.getArgs().hasArg(OPT___help_hidden))
        {
            printHelp(c.getArgs().hasArg(OPT___help_hidden));
            return false;
        }

        if (c.getArgs().hasArg(OPT___version))
        {
            printVersion(c, System.out);
            return false;
        }

        if (c.getArgs().hasArg(OPT__v) || c.getArgs().hasArg(OPT___HASH_HASH_HASH))
        {
            printVersion(c, System.err);
            suppressMissingInputWarning = true;
        }
        return true;
    }

    public Compilation buildCompilation(String[] args)
    {
        InputArgList argList = parseArgList(args);
        host = getHostInfo(triple);

        Compilation c = new Compilation(this, host.selectToolChain(argList),
                argList);

        if (!handleVersion(c))
            return c;

        // Builds a sequence of Actions to be performed, like preprocess,
        // precompile, compile, assembly, linking etc.        
        buildActions(c);

        buildJobs(c);
        return c;
    }

    public int executeCompilation(Compilation c)
    {
        Job.Command failureCmd = null;

        int res = c.executeJob();
        failureCmd = c.getFailureCommand();
        if (res != 0)
        {
            clearTemporaryFiles();
        }

        if (res != 0)
        {
            Action source = failureCmd.getSource();
            boolean isFriendlyTool = source instanceof PreprocessJobAction ||
                    source instanceof PrecompileJobAction ||
                    source instanceof CompileJobAction;
            if (!isFriendlyTool || res != 1)
            {
                if (res < 0)
                    diag(err_drv_command_signalled)
                    .addString(source.getClassName()).emit();
                else
                    diag(err_drv_command_failed)
                    .addString(source.getClassName()).emit();
            }
        }
        return res;
    }

    private void clearTemporaryFiles()
    {

    }
}
